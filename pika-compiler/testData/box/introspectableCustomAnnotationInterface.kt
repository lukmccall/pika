// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

annotation class OptimizedRecord

@OptimizedRecord
interface RecordWithId {
  val id: String
}

@Introspectable
class UserRecord(override val id: String, val email: String) : RecordWithId

fun box(): String {
  val user = UserRecord("u1", "alice@test.com")

  if (user !is PIntrospectionProvider) return "FAIL: UserRecord should implement PIntrospectionProvider"

  @Suppress("UNCHECKED_CAST")
  val data = (user as PIntrospectionProvider).getIntrospectionData() as PIntrospectionData<UserRecord>
  if (data.jClass != UserRecord::class.java) return "FAIL: jClass should be UserRecord, got ${data.jClass}"
  if (data.properties.size != 2) return "FAIL: expected 2 properties, got ${data.properties.size}"

  val idProp = data.properties.find { it.name == "id" }
    ?: return "FAIL: id property not found"
  if (idProp.get(user) != "u1") return "FAIL: id getter expected u1 but got ${idProp.get(user)}"

  val emailProp = data.properties.find { it.name == "email" }
    ?: return "FAIL: email property not found"
  if (emailProp.get(user) != "alice@test.com") return "FAIL: email getter expected alice@test.com"

  return "OK"
}
