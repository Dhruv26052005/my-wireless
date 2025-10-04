package com.hybridmesh.chat.network.encryption

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionManager(private val context: Context) {
    
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private val prefs: SharedPreferences = context.getSharedPreferences("mesh_encryption", Context.MODE_PRIVATE)
    
    private val keyAlias = "mesh_key_pair"
    private val gcmIvLength = 12
    private val gcmTagLength = 16
    
    init {
        keyStore.load(null)
        generateKeyPairIfNeeded()
    }
    
    data class EncryptedMessage(
        val content: ByteArray,
        val iv: ByteArray,
        val tag: ByteArray,
        val senderPublicKey: ByteArray,
        val recipientPublicKey: ByteArray,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as EncryptedMessage
            
            if (!content.contentEquals(other.content)) return false
            if (!iv.contentEquals(other.iv)) return false
            if (!tag.contentEquals(other.tag)) return false
            if (!senderPublicKey.contentEquals(other.senderPublicKey)) return false
            if (!recipientPublicKey.contentEquals(other.recipientPublicKey)) return false
            if (timestamp != other.timestamp) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = content.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            result = 31 * result + tag.contentHashCode()
            result = 31 * result + senderPublicKey.contentHashCode()
            result = 31 * result + recipientPublicKey.contentHashCode()
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }
    
    fun getPublicKey(): PublicKey? {
        return try {
            keyStore.getCertificate(keyAlias)?.publicKey
        } catch (e: Exception) {
            null
        }
    }
    
    fun getPublicKeyBytes(): ByteArray? {
        return getPublicKey()?.encoded
    }
    
    fun getPublicKeyString(): String? {
        return getPublicKeyBytes()?.let { Base64.getEncoder().encodeToString(it) }
    }
    
    fun encryptMessage(message: String, recipientPublicKeyBytes: ByteArray): EncryptedMessage? {
        return try {
            val recipientPublicKey = java.security.spec.X509EncodedKeySpec(recipientPublicKeyBytes).let { spec ->
                java.security.KeyFactory.getInstance("EC").generatePublic(spec)
            }
            
            // Generate shared secret using ECDH
            val sharedSecret = generateSharedSecret(recipientPublicKey)
            
            // Generate AES key from shared secret
            val aesKey = generateAESKey(sharedSecret)
            
            // Encrypt message with AES-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = generateRandomBytes(gcmIvLength)
            val gcmSpec = GCMParameterSpec(gcmTagLength * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
            
            val encryptedContent = cipher.doFinal(message.toByteArray())
            
            // Split encrypted content and tag
            val content = encryptedContent.sliceArray(0 until encryptedContent.size - gcmTagLength)
            val tag = encryptedContent.sliceArray(encryptedContent.size - gcmTagLength until encryptedContent.size)
            
            EncryptedMessage(
                content = content,
                iv = iv,
                tag = tag,
                senderPublicKey = getPublicKeyBytes() ?: return null,
                recipientPublicKey = recipientPublicKeyBytes
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun decryptMessage(encryptedMessage: EncryptedMessage): String? {
        return try {
            val senderPublicKey = java.security.spec.X509EncodedKeySpec(encryptedMessage.senderPublicKey).let { spec ->
                java.security.KeyFactory.getInstance("EC").generatePublic(spec)
            }
            
            // Generate shared secret using ECDH
            val sharedSecret = generateSharedSecret(senderPublicKey)
            
            // Generate AES key from shared secret
            val aesKey = generateAESKey(sharedSecret)
            
            // Decrypt message with AES-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(gcmTagLength * 8, encryptedMessage.iv)
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec)
            
            // Combine content and tag
            val encryptedContent = encryptedMessage.content + encryptedMessage.tag
            val decryptedBytes = cipher.doFinal(encryptedContent)
            
            String(decryptedBytes)
        } catch (e: Exception) {
            null
        }
    }
    
    fun encryptMessageForRelay(message: String, nextHopPublicKey: ByteArray): EncryptedMessage? {
        // For relay messages, we encrypt with the next hop's public key
        // The relay cannot decrypt the original message, only forward it
        return encryptMessage(message, nextHopPublicKey)
    }
    
    fun decryptRelayMessage(encryptedMessage: EncryptedMessage): String? {
        // Decrypt relay message to get the original encrypted message
        return decryptMessage(encryptedMessage)
    }
    
    private fun generateKeyPairIfNeeded() {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_AGREE_KEY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
            
            keyPairGenerator.initialize(keyGenParameterSpec)
            keyPairGenerator.generateKeyPair()
        }
    }
    
    private fun generateSharedSecret(publicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        val privateKey = keyStore.getKey(keyAlias, null) as PrivateKey
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)
        return keyAgreement.generateSecret()
    }
    
    private fun generateAESKey(sharedSecret: ByteArray): SecretKeySpec {
        // Use HKDF to derive AES key from shared secret
        val hkdf = HKDF()
        val aesKey = hkdf.expand(sharedSecret, "AES_KEY".toByteArray(), 32)
        return SecretKeySpec(aesKey, "AES")
    }
    
    private fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes
    }
    
    fun storePublicKey(deviceId: String, publicKeyBytes: ByteArray) {
        val publicKeyString = Base64.getEncoder().encodeToString(publicKeyBytes)
        prefs.edit().putString("public_key_$deviceId", publicKeyString).apply()
    }
    
    fun getStoredPublicKey(deviceId: String): ByteArray? {
        val publicKeyString = prefs.getString("public_key_$deviceId", null) ?: return null
        return try {
            Base64.getDecoder().decode(publicKeyString)
        } catch (e: Exception) {
            null
        }
    }
    
    fun removeStoredPublicKey(deviceId: String) {
        prefs.edit().remove("public_key_$deviceId").apply()
    }
    
    fun clearAllStoredKeys() {
        prefs.edit().clear().apply()
    }
}

// Simple HKDF implementation
private class HKDF {
    fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val hashLength = 32 // SHA-256 output length
        val n = (length + hashLength - 1) / hashLength
        val result = ByteArray(length)
        var offset = 0
        
        for (i in 1..n) {
            val input = ByteArray(hashLength + info.size + 1)
            if (i > 1) {
                System.arraycopy(result, offset - hashLength, input, 0, hashLength)
            }
            System.arraycopy(info, 0, input, hashLength, info.size)
            input[input.size - 1] = i.toByte()
            
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(input)
            val copyLength = kotlin.math.min(hashLength, length - offset)
            System.arraycopy(hash, 0, result, offset, copyLength)
            offset += copyLength
        }
        
        return result
    }
}
