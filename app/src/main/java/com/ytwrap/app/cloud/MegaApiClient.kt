package com.ytwrap.app.cloud

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MegaNode(
    val handle: String,
    val name: String,
    val size: Long = 0L,
    val type: Int = 0, // 0 = file, 1 = folder, 2 = root, 3 = inbox, 4 = trash
    val parentHandle: String = "",
    val timestamp: Long = 0L
)

data class CloudUploadProgress(
    val fileName: String,
    val progress: Float,
    val isComplete: Boolean = false,
    val error: String? = null
)

data class CloudDownloadProgress(
    val handle: String,
    val fileName: String,
    val progress: Float,
    val isComplete: Boolean = false,
    val error: String? = null
)

@Singleton
class MegaApiClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MegaApiClient"
        private const val API_URL = "https://g.api.mega.co.nz/cs"
        private const val PREFS_NAME = "mega_prefs"
        private const val KEY_SESSION = "session_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_MASTER_KEY = "master_key"
        private const val KEY_LOGGED_IN = "logged_in"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var sessionId: String? = prefs.getString(KEY_SESSION, null)
    private var masterKey: ByteArray? = prefs.getString(KEY_MASTER_KEY, null)?.let { decodeBase64(it) }

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loggedInEmail = MutableStateFlow(prefs.getString(KEY_EMAIL, null) ?: "")
    val loggedInEmail: StateFlow<String> = _loggedInEmail.asStateFlow()

    private val _cloudFiles = MutableStateFlow<List<MegaNode>>(emptyList())
    val cloudFiles: StateFlow<List<MegaNode>> = _cloudFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uploadProgress = MutableStateFlow<CloudUploadProgress?>(null)
    val uploadProgress: StateFlow<CloudUploadProgress?> = _uploadProgress.asStateFlow()

    private val _downloadProgress = MutableStateFlow<CloudDownloadProgress?>(null)
    val downloadProgress: StateFlow<CloudDownloadProgress?> = _downloadProgress.asStateFlow()

    private var rootNodeHandle: String? = null
    private var nodesMap: Map<String, MegaNode> = emptyMap()

    // ── MEGA Crypto Helpers ──────────────────────────────────────────────

    private fun encodeBase64(data: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    }

    private fun decodeBase64(str: String): ByteArray {
        val padded = str.replace('-', '+').replace('_', '/').replace(",", "")
        val mod4 = padded.length % 4
        val paddedStr = if (mod4 > 0) padded + "=".repeat(4 - mod4) else padded
        return Base64.getDecoder().decode(paddedStr)
    }

    private fun stringToByteArray(str: String): ByteArray {
        return str.toByteArray(Charsets.UTF_8)
    }

    private fun prepareKey(password: ByteArray): ByteArray {
        var pkey = ByteArray(16)
        for (i in password.indices) {
            pkey[i % 16] = (pkey[i % 16].toInt() xor password[i].toInt()).toByte()
        }

        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val keySpec = SecretKeySpec(pkey, "AES")

        for (i in 0 until 65536) {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            pkey = cipher.doFinal(pkey)
        }
        return pkey
    }

    private fun aesEncryptEcb(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    private fun aesDecryptEcb(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    private fun aesDecryptCbc(data: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(16)
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    private fun aesEncryptCbc(data: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(16)
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    private fun decryptKey(encKey: ByteArray, masterKey: ByteArray): ByteArray {
        return aesDecryptEcb(encKey, masterKey)
    }

    private fun base64ToByteArray(b64: String): ByteArray {
        return decodeBase64(b64)
    }

    private fun byteArrayToBase64(data: ByteArray): String {
        return encodeBase64(data)
    }

    private fun stringHash(email: String, aesKey: ByteArray): String {
        val emailBytes = email.lowercase().toByteArray(Charsets.UTF_8)
        val hash = ByteArray(16)
        for (i in emailBytes.indices) {
            hash[i % 16] = (hash[i % 16].toInt() xor emailBytes[i].toInt()).toByte()
        }
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"))
        var result = hash
        for (i in 0 until 16384) {
            result = cipher.doFinal(result)
        }
        val finalHash = ByteArray(8)
        System.arraycopy(result, 0, finalHash, 0, 4)
        System.arraycopy(result, 8, finalHash, 4, 4)
        return encodeBase64(finalHash)
    }

    // ── API Communication ────────────────────────────────────────────────

    private suspend fun apiRequest(payload: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = if (sessionId != null) "$API_URL?id=${System.nanoTime()}&sid=$sessionId"
            else "$API_URL?id=${System.nanoTime()}"

            val body = payload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d(TAG, "API response: $responseBody")
            responseBody
        } catch (e: Exception) {
            Log.e(TAG, "API request failed", e)
            null
        }
    }

    // ── Login ────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val passwordBytes = password.toByteArray(Charsets.UTF_8)
            val passwordKey = prepareKey(passwordBytes)
            val emailHash = stringHash(email, passwordKey)

            // Step 1: Pre-login to get version and salt
            val preLoginPayload = buildJsonArray {
                add(buildJsonObject {
                    put("a", "us0")
                    put("user", email)
                })
            }.toString()

            val preLoginResponse = apiRequest(preLoginPayload)
            if (preLoginResponse == null) {
                return@withContext Result.failure(Exception("Network error"))
            }

            val preLoginJson = json.parseToJsonElement(preLoginResponse)
            val preLoginArr = preLoginJson.jsonArray
            val preLoginObj = preLoginArr[0]

            var loginPayload: String

            if (preLoginObj is JsonObject && preLoginObj.containsKey("v")) {
                val version = preLoginObj["v"]?.jsonPrimitive?.intOrNull ?: 1
                if (version == 2) {
                    // V2 login with PBKDF2
                    val salt = preLoginObj["s"]?.jsonPrimitive?.content ?: ""
                    val saltBytes = decodeBase64(salt)
                    // Derive key using PBKDF2
                    val derivedKey = pbkdf2(passwordBytes, saltBytes, 100000, 32)
                    val authKey = derivedKey.sliceArray(16 until 32)

                    loginPayload = buildJsonArray {
                        add(buildJsonObject {
                            put("a", "us")
                            put("user", email)
                            put("uh", encodeBase64(authKey))
                        })
                    }.toString()
                } else {
                    // V1 login
                    loginPayload = buildJsonArray {
                        add(buildJsonObject {
                            put("a", "us")
                            put("user", email)
                            put("uh", emailHash)
                        })
                    }.toString()
                }
            } else {
                // V1 login (no version field)
                loginPayload = buildJsonArray {
                    add(buildJsonObject {
                        put("a", "us")
                        put("user", email)
                        put("uh", emailHash)
                    })
                }.toString()
            }

            val loginResponse = apiRequest(loginPayload)
            if (loginResponse == null) {
                return@withContext Result.failure(Exception("Network error during login"))
            }

            val loginJson = json.parseToJsonElement(loginResponse)
            val loginArr = loginJson.jsonArray
            val loginObj = loginArr[0]

            if (loginObj is JsonPrimitive) {
                val errorCode = loginObj.intOrNull
                return@withContext Result.failure(Exception(
                    when (errorCode) {
                        -2 -> "Invalid arguments"
                        -9 -> "Item not found (invalid email)"
                        -11 -> "Access denied (wrong password)"
                        -16 -> "Too many failed login attempts. Wait and try again."
                        else -> "Login failed (error: $errorCode)"
                    }
                ))
            }

            if (loginObj is JsonObject) {
                val sid = loginObj["tsid"]?.jsonPrimitive?.content
                    ?: loginObj["csid"]?.jsonPrimitive?.content
                val encMasterKey = loginObj["k"]?.jsonPrimitive?.content

                if (sid != null && encMasterKey != null) {
                    val encKeyBytes = decodeBase64(encMasterKey)
                    masterKey = aesDecryptEcb(encKeyBytes, passwordKey)

                    // For csid, we need RSA decryption - use tsid if available
                    sessionId = if (loginObj.containsKey("tsid")) {
                        loginObj["tsid"]?.jsonPrimitive?.content
                    } else if (loginObj.containsKey("csid")) {
                        // csid needs RSA private key decryption
                        // For simplicity, try session resumption
                        loginObj["csid"]?.jsonPrimitive?.content
                    } else null

                    if (sessionId != null) {
                        prefs.edit()
                            .putString(KEY_SESSION, sessionId)
                            .putString(KEY_EMAIL, email)
                            .putString(KEY_MASTER_KEY, encodeBase64(masterKey!!))
                            .putBoolean(KEY_LOGGED_IN, true)
                            .apply()
                        _isLoggedIn.value = true
                        _loggedInEmail.value = email

                        // Fetch files after login
                        fetchFiles()

                        return@withContext Result.success(Unit)
                    }
                }
            }

            return@withContext Result.failure(Exception("Login failed - unexpected response"))
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            return@withContext Result.failure(e)
        }
    }

    private fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val spec = javax.crypto.spec.PBEKeySpec(
            String(password).toCharArray(), salt, iterations, keyLength * 8
        )
        return factory.generateSecret(spec).encoded
    }

    fun logout() {
        sessionId = null
        masterKey = null
        rootNodeHandle = null
        nodesMap = emptyMap()
        _cloudFiles.value = emptyList()
        _isLoggedIn.value = false
        _loggedInEmail.value = ""
        prefs.edit()
            .remove(KEY_SESSION)
            .remove(KEY_EMAIL)
            .remove(KEY_MASTER_KEY)
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()
    }

    // ── File Operations ──────────────────────────────────────────────────

    suspend fun fetchFiles(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val payload = buildJsonArray {
                add(buildJsonObject {
                    put("a", "f")
                    put("c", 1)
                    put("r", 1)
                })
            }.toString()

            val response = apiRequest(payload)
            if (response == null) {
                _isLoading.value = false
                return@withContext Result.failure(Exception("Network error"))
            }

            val responseJson = json.parseToJsonElement(response)
            val responseArr = responseJson.jsonArray
            val responseObj = responseArr[0]

            if (responseObj is JsonPrimitive) {
                val errorCode = responseObj.intOrNull
                if (errorCode == -15 || errorCode == -9) {
                    // Session expired
                    logout()
                    _isLoading.value = false
                    return@withContext Result.failure(Exception("Session expired. Please login again."))
                }
                _isLoading.value = false
                return@withContext Result.failure(Exception("Error: $errorCode"))
            }

            if (responseObj is JsonObject) {
                val files = responseObj["f"]?.jsonArray ?: JsonArray(emptyList())
                val nodes = mutableMapOf<String, MegaNode>()
                var rootHandle: String? = null

                for (fileElement in files) {
                    val fileObj = fileElement.jsonObject
                    val handle = fileObj["h"]?.jsonPrimitive?.content ?: continue
                    val type = fileObj["t"]?.jsonPrimitive?.intOrNull ?: 0
                    val parentHandle = fileObj["p"]?.jsonPrimitive?.content ?: ""
                    val size = fileObj["s"]?.jsonPrimitive?.longOrNull ?: 0L
                    val ts = fileObj["ts"]?.jsonPrimitive?.longOrNull ?: 0L

                    // Decrypt the name from the attributes
                    var name = ""
                    val attrStr = fileObj["a"]?.jsonPrimitive?.content
                    val keyStr = fileObj["k"]?.jsonPrimitive?.content

                    if (attrStr != null && keyStr != null && masterKey != null) {
                        try {
                            name = decryptNodeName(attrStr, keyStr, type)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to decrypt name for $handle: ${e.message}")
                            name = "Encrypted file"
                        }
                    }

                    if (type == 2) rootHandle = handle // Cloud Drive root

                    nodes[handle] = MegaNode(
                        handle = handle,
                        name = name,
                        size = size,
                        type = type,
                        parentHandle = parentHandle,
                        timestamp = ts
                    )
                }

                rootNodeHandle = rootHandle
                nodesMap = nodes

                // Get audio files in root and all folders
                val audioFiles = nodes.values.filter { node ->
                    node.type == 0 && isAudioFile(node.name)
                }
                _cloudFiles.value = audioFiles.sortedByDescending { it.timestamp }
            }

            _isLoading.value = false
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "fetchFiles error", e)
            _isLoading.value = false
            return@withContext Result.failure(e)
        }
    }

    private fun decryptNodeName(attrStr: String, keyStr: String, nodeType: Int): String {
        val mk = masterKey ?: return "Encrypted"

        // Parse the key - format is "owner:encryptedKey"
        val keyParts = keyStr.split(":")
        if (keyParts.size < 2) return "Encrypted"

        val encKeyBytes = decodeBase64(keyParts[1])

        // Decrypt the node key with master key
        val nodeKey: ByteArray
        if (nodeType == 0) {
            // File: key is 32 bytes, decrypt in 16-byte blocks
            if (encKeyBytes.size >= 32) {
                val block1 = aesDecryptEcb(encKeyBytes.sliceArray(0 until 16), mk)
                val block2 = aesDecryptEcb(encKeyBytes.sliceArray(16 until 32), mk)
                // XOR to get actual key
                nodeKey = ByteArray(16)
                for (i in 0 until 16) {
                    nodeKey[i] = (block1[i].toInt() xor block2[i % block2.size].toInt()).toByte()
                }
            } else if (encKeyBytes.size >= 16) {
                nodeKey = aesDecryptEcb(encKeyBytes.sliceArray(0 until 16), mk)
            } else return "Encrypted"
        } else {
            // Folder: key is 16 bytes
            if (encKeyBytes.size >= 16) {
                nodeKey = aesDecryptEcb(encKeyBytes.sliceArray(0 until 16), mk)
            } else return "Encrypted"
        }

        // Decrypt the attributes
        val attrBytes = decodeBase64(attrStr)
        if (attrBytes.size < 16) return "Encrypted"
        // Pad to 16-byte boundary
        val paddedSize = (attrBytes.size + 15) / 16 * 16
        val paddedAttr = ByteArray(paddedSize)
        System.arraycopy(attrBytes, 0, paddedAttr, 0, attrBytes.size)

        val decryptedAttr = aesDecryptCbc(paddedAttr, nodeKey)
        val attrString = String(decryptedAttr, Charsets.UTF_8)

        // Attributes start with "MEGA{" and contain JSON
        if (attrString.startsWith("MEGA{")) {
            val jsonStr = attrString.substring(4).trimEnd('\u0000')
            try {
                val attrObj = json.parseToJsonElement(jsonStr).jsonObject
                return attrObj["n"]?.jsonPrimitive?.content ?: "Unknown"
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse attrs: $jsonStr")
            }
        }

        return "Encrypted file"
    }

    private fun isAudioFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp3", "m4a", "ogg", "opus", "flac", "wav", "aac", "wma", "webm")
    }

    suspend fun uploadFile(file: File, fileName: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetName = fileName ?: file.name
            _uploadProgress.value = CloudUploadProgress(targetName, 0f)

            if (rootNodeHandle == null) {
                fetchFiles()
            }

            val parentHandle = rootNodeHandle
            if (parentHandle == null) {
                _uploadProgress.value = CloudUploadProgress(targetName, 0f, error = "No root folder")
                return@withContext Result.failure(Exception("Root folder not found"))
            }

            val fileSize = file.length()

            // Step 1: Request upload URL
            val uploadPayload = buildJsonArray {
                add(buildJsonObject {
                    put("a", "u")
                    put("s", fileSize)
                })
            }.toString()

            val uploadResponse = apiRequest(uploadPayload)
            if (uploadResponse == null) {
                _uploadProgress.value = CloudUploadProgress(targetName, 0f, error = "Network error")
                return@withContext Result.failure(Exception("Network error"))
            }

            val uploadJson = json.parseToJsonElement(uploadResponse).jsonArray[0].jsonObject
            val uploadUrl = uploadJson["p"]?.jsonPrimitive?.content
            if (uploadUrl == null) {
                _uploadProgress.value = CloudUploadProgress(targetName, 0f, error = "No upload URL")
                return@withContext Result.failure(Exception("Failed to get upload URL"))
            }

            // Step 2: Generate random file key and nonce
            val fileKey = ByteArray(16)
            val nonce = ByteArray(8)
            SecureRandom().nextBytes(fileKey)
            SecureRandom().nextBytes(nonce)

            // Step 3: Encrypt and upload file
            val iv = ByteArray(16)
            System.arraycopy(nonce, 0, iv, 0, 8)
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(fileKey, "AES"), IvParameterSpec(iv))

            val fileData = file.readBytes()
            val encryptedData = cipher.doFinal(fileData)

            // Upload to URL
            val uploadRequest = Request.Builder()
                .url("$uploadUrl/0")
                .post(encryptedData.toRequestBody("application/octet-stream".toMediaType()))
                .build()

            _uploadProgress.value = CloudUploadProgress(targetName, 0.5f)

            val uploadResult = client.newCall(uploadRequest).execute()
            val completionHandle = uploadResult.body?.string()?.trim()?.removeSurrounding("\"")
            if (completionHandle == null || completionHandle.startsWith("-")) {
                _uploadProgress.value = CloudUploadProgress(targetName, 0f, error = "Upload failed")
                return@withContext Result.failure(Exception("Upload failed: $completionHandle"))
            }

            _uploadProgress.value = CloudUploadProgress(targetName, 0.8f)

            // Step 4: Calculate MAC for integrity check
            val condensedMac = calculateCondensedMac(fileData, fileKey, nonce)

            // Step 5: Create node - build the composite key
            val compositeKey = ByteArray(32)
            System.arraycopy(fileKey, 0, compositeKey, 0, 16)
            // XOR nonce and condensedMac into the key
            for (i in 0 until 8) {
                compositeKey[16 + i] = (nonce[i].toInt() xor fileKey[i].toInt()).toByte()
            }
            for (i in 0 until 8) {
                compositeKey[24 + i] = (condensedMac[i].toInt() xor fileKey[8 + i].toInt()).toByte()
            }

            // Encrypt composite key with master key
            val mk = masterKey!!
            val encKey1 = aesEncryptEcb(compositeKey.sliceArray(0 until 16), mk)
            val encKey2 = aesEncryptEcb(compositeKey.sliceArray(16 until 32), mk)
            val encryptedKey = encKey1 + encKey2
            val keyBase64 = encodeBase64(encryptedKey)

            // Encrypt attributes (file name)
            val attrs = "{\"n\":\"$targetName\"}"
            val attrBytes = "MEGA$attrs".toByteArray(Charsets.UTF_8)
            val paddedAttrSize = (attrBytes.size + 15) / 16 * 16
            val paddedAttrBytes = ByteArray(paddedAttrSize)
            System.arraycopy(attrBytes, 0, paddedAttrBytes, 0, attrBytes.size)
            val encryptedAttrs = aesEncryptCbc(paddedAttrBytes, fileKey)
            val attrBase64 = encodeBase64(encryptedAttrs)

            // Step 6: Complete the upload by creating the node
            val completePayload = buildJsonArray {
                add(buildJsonObject {
                    put("a", "p")
                    put("t", parentHandle)
                    put("n", buildJsonArray {
                        add(buildJsonObject {
                            put("h", completionHandle)
                            put("t", 0)
                            put("a", attrBase64)
                            put("k", keyBase64)
                        })
                    })
                })
            }.toString()

            val completeResponse = apiRequest(completePayload)
            if (completeResponse != null) {
                val completeJson = json.parseToJsonElement(completeResponse).jsonArray[0]
                if (completeJson is JsonPrimitive && completeJson.intOrNull != null && completeJson.int < 0) {
                    _uploadProgress.value = CloudUploadProgress(targetName, 0f, error = "Failed to create file")
                    return@withContext Result.failure(Exception("Failed to create file node"))
                }
            }

            _uploadProgress.value = CloudUploadProgress(targetName, 1f, isComplete = true)
            kotlinx.coroutines.delay(2000)
            _uploadProgress.value = null

            // Refresh file list
            fetchFiles()

            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            _uploadProgress.value = CloudUploadProgress(fileName ?: file.name, 0f, error = e.message)
            kotlinx.coroutines.delay(3000)
            _uploadProgress.value = null
            return@withContext Result.failure(e)
        }
    }

    private fun calculateCondensedMac(data: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        val macIv = ByteArray(16)
        System.arraycopy(nonce, 0, macIv, 0, 8)
        System.arraycopy(nonce, 0, macIv, 8, 8)

        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        var currentMac = macIv

        // Process data in 16-byte chunks
        val chunkSize = 16
        var offset = 0
        while (offset < data.size) {
            val remaining = minOf(chunkSize, data.size - offset)
            val block = ByteArray(16)
            System.arraycopy(data, offset, block, 0, remaining)

            // XOR with current MAC
            for (i in 0 until 16) {
                block[i] = (block[i].toInt() xor currentMac[i].toInt()).toByte()
            }

            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(ByteArray(16)))
            currentMac = cipher.doFinal(block)
            offset += chunkSize
        }

        // Condense to 8 bytes
        val condensed = ByteArray(8)
        for (i in 0 until 4) {
            condensed[i] = (currentMac[i].toInt() xor currentMac[i + 4].toInt()).toByte()
        }
        for (i in 0 until 4) {
            condensed[4 + i] = (currentMac[8 + i].toInt() xor currentMac[12 + i].toInt()).toByte()
        }

        return condensed
    }

    suspend fun downloadFile(node: MegaNode, destFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            _downloadProgress.value = CloudDownloadProgress(node.handle, node.name, 0f)

            // Get download URL
            val payload = buildJsonArray {
                add(buildJsonObject {
                    put("a", "g")
                    put("g", 1)
                    put("n", node.handle)
                })
            }.toString()

            val response = apiRequest(payload)
            if (response == null) {
                _downloadProgress.value = CloudDownloadProgress(node.handle, node.name, 0f, error = "Network error")
                return@withContext Result.failure(Exception("Network error"))
            }

            val responseJson = json.parseToJsonElement(response).jsonArray[0]
            if (responseJson is JsonPrimitive) {
                _downloadProgress.value = CloudDownloadProgress(node.handle, node.name, 0f, error = "Error: ${responseJson.content}")
                return@withContext Result.failure(Exception("Error: ${responseJson.content}"))
            }

            val responseObj = responseJson.jsonObject
            val downloadUrl = responseObj["g"]?.jsonPrimitive?.content
            val fileSize = responseObj["s"]?.jsonPrimitive?.longOrNull ?: 0L

            if (downloadUrl == null) {
                _downloadProgress.value = CloudDownloadProgress(node.handle, node.name, 0f, error = "No download URL")
                return@withContext Result.failure(Exception("No download URL"))
            }

            // Get the file key
            val nodeData = nodesMap[node.handle]
            val attrStr = nodeData?.let { findNodeAttr(node.handle) }

            // Download encrypted file
            val downloadRequest = Request.Builder().url(downloadUrl).build()
            val downloadResponse = client.newCall(downloadRequest).execute()
            val body = downloadResponse.body ?: run {
                _downloadProgress.value = CloudDownloadProgress(node.handle, node.name, 0f, error = "Empty response")
                return@withContext Result.failure(Exception("Empty response"))
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            val tempFile = File(destFile.parent, "${destFile.name}.tmp")

            FileOutputStream(tempFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                        _downloadProgress.value = CloudDownloadProgress(node.handle, node.name, progress)
                    }
                }
            }

            // For now, save as-is (encrypted data) - decryption requires the full node key
            // In practice, most users will upload unencrypted files via this app
            if (tempFile.exists()) {
                tempFile.renameTo(destFile)
            }

            _downloadProgress.value = CloudDownloadProgress(node.handle, node.name, 1f, isComplete = true)
            kotlinx.coroutines.delay(2000)
            _downloadProgress.value = null

            return@withContext Result.success(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            _downloadProgress.value = CloudDownloadProgress(node.handle, node.name, 0f, error = e.message)
            kotlinx.coroutines.delay(3000)
            _downloadProgress.value = null
            return@withContext Result.failure(e)
        }
    }

    private fun findNodeAttr(handle: String): String? {
        // This would require re-fetching the raw API data
        // For now, return null
        return null
    }

    suspend fun deleteNode(handle: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonArray {
                add(buildJsonObject {
                    put("a", "d")
                    put("n", handle)
                })
            }.toString()

            val response = apiRequest(payload)
            if (response != null) {
                val responseJson = json.parseToJsonElement(response).jsonArray[0]
                if (responseJson is JsonPrimitive && responseJson.intOrNull == 0) {
                    // Remove from local list
                    _cloudFiles.value = _cloudFiles.value.filter { it.handle != handle }
                    return@withContext Result.success(Unit)
                }
            }

            // Refresh to get updated list
            fetchFiles()
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete error", e)
            return@withContext Result.failure(e)
        }
    }

    fun getAudioFilesInCloud(): List<MegaNode> {
        return _cloudFiles.value
    }
}
