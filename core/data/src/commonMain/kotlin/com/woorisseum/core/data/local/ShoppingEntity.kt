package com.woorisseum.core.data.local

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "shopping_settings")
internal data class ShoppingSettingsEntity(@PrimaryKey val id: Int = 1)

@Entity(tableName = "shopping_categories", indices = [Index(value = ["nameKey"], unique = true)])
internal data class ShoppingCategoryEntity(@PrimaryKey val id: String, val name: String,
    val nameKey: String, val position: Long, val revision: Long = 0)

@Entity(tableName = "shopping_items", indices = [Index(value = ["id"], unique = true),
    Index(value = ["name"], unique = true), Index(value = ["categoryId"])],
    foreignKeys = [ForeignKey(entity = ShoppingCategoryEntity::class, parentColumns = ["id"],
        childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT)])
internal data class ShoppingItemEntity(@PrimaryKey(autoGenerate = true) val registrationOrder: Long = 0,
    val id: String, val name: String, val quantity: String?, val memo: String?, val purchased: Boolean,
    val categoryId: String?, val revision: Long = 0)
