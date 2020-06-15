// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto
package com.google.protobuf

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.sanitize
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

/**
 * Describes a value within an enum.
 */
class EnumValueDescriptorProto(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  @JvmField
  val name: String? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  @JvmField
  val number: Int? = null,
  @field:WireField(
    tag = 3,
    adapter = "com.google.protobuf.EnumValueOptions#ADAPTER"
  )
  @JvmField
  val options: EnumValueOptions? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<EnumValueDescriptorProto, EnumValueDescriptorProto.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.name = name
    builder.number = number
    builder.options = options
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is EnumValueDescriptorProto) return false
    if (unknownFields != other.unknownFields) return false
    if (name != other.name) return false
    if (number != other.number) return false
    if (options != other.options) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + name.hashCode()
      result = result * 37 + number.hashCode()
      result = result * 37 + options.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (name != null) result += """name=${sanitize(name)}"""
    if (number != null) result += """number=$number"""
    if (options != null) result += """options=$options"""
    return result.joinToString(prefix = "EnumValueDescriptorProto{", separator = ", ", postfix =
        "}")
  }

  fun copy(
    name: String? = this.name,
    number: Int? = this.number,
    options: EnumValueOptions? = this.options,
    unknownFields: ByteString = this.unknownFields
  ): EnumValueDescriptorProto = EnumValueDescriptorProto(name, number, options, unknownFields)

  class Builder : Message.Builder<EnumValueDescriptorProto, Builder>() {
    @JvmField
    var name: String? = null

    @JvmField
    var number: Int? = null

    @JvmField
    var options: EnumValueOptions? = null

    fun name(name: String?): Builder {
      this.name = name
      return this
    }

    fun number(number: Int?): Builder {
      this.number = number
      return this
    }

    fun options(options: EnumValueOptions?): Builder {
      this.options = options
      return this
    }

    override fun build(): EnumValueDescriptorProto = EnumValueDescriptorProto(
      name = name,
      number = number,
      options = options,
      unknownFields = buildUnknownFields()
    )
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<EnumValueDescriptorProto> = object :
        ProtoAdapter<EnumValueDescriptorProto>(
      FieldEncoding.LENGTH_DELIMITED, 
      EnumValueDescriptorProto::class, 
      "type.googleapis.com/google.protobuf.EnumValueDescriptorProto"
    ) {
      override fun encodedSize(value: EnumValueDescriptorProto): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
        size += ProtoAdapter.INT32.encodedSizeWithTag(2, value.number)
        size += EnumValueOptions.ADAPTER.encodedSizeWithTag(3, value.options)
        return size
      }

      override fun encode(writer: ProtoWriter, value: EnumValueDescriptorProto) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
        ProtoAdapter.INT32.encodeWithTag(writer, 2, value.number)
        EnumValueOptions.ADAPTER.encodeWithTag(writer, 3, value.options)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): EnumValueDescriptorProto {
        var name: String? = null
        var number: Int? = null
        var options: EnumValueOptions? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> name = ProtoAdapter.STRING.decode(reader)
            2 -> number = ProtoAdapter.INT32.decode(reader)
            3 -> options = EnumValueOptions.ADAPTER.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return EnumValueDescriptorProto(
          name = name,
          number = number,
          options = options,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: EnumValueDescriptorProto): EnumValueDescriptorProto = value.copy(
        options = value.options?.let(EnumValueOptions.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
