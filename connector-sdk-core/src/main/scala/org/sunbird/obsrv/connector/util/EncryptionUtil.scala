package org.sunbird.obsrv.connector.util

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class EncryptionUtil(encryptionKey: String) {

  private val algorithm = "AES"

  private val decryptInstance = getInstance(Cipher.DECRYPT_MODE)

  def decrypt(value: String): String = {
    val decryptedValue64 = Base64.getDecoder.decode(value)
    val decryptedByteValue = decryptInstance.doFinal(decryptedValue64)
    new String(decryptedByteValue, "utf-8")
  }

  private def getInstance(mode: Int): Cipher = {
    val cipher = Cipher.getInstance(algorithm)
    val key = new SecretKeySpec(encryptionKey.getBytes("utf-8"), algorithm)
    cipher.init(mode, key)
    cipher
  }
}
