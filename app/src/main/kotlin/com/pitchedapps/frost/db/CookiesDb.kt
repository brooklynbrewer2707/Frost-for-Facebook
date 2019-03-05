/*
 * Copyright 2018 Allan Wang
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pitchedapps.frost.db

import android.os.Parcelable
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pitchedapps.frost.utils.L
import com.raizlabs.android.dbflow.annotation.ConflictAction
import com.raizlabs.android.dbflow.annotation.Database
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.kotlinextensions.async
import com.raizlabs.android.dbflow.kotlinextensions.delete
import com.raizlabs.android.dbflow.kotlinextensions.eq
import com.raizlabs.android.dbflow.kotlinextensions.from
import com.raizlabs.android.dbflow.kotlinextensions.save
import com.raizlabs.android.dbflow.kotlinextensions.select
import com.raizlabs.android.dbflow.kotlinextensions.where
import com.raizlabs.android.dbflow.structure.BaseModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by Allan Wang on 2017-05-30.
 */

@Entity(tableName = "cookies")
@Parcelize
data class CookieEntity(
    @androidx.room.PrimaryKey
    var id: Long,
    var name: String,
    var cookie: String
) : Parcelable {
    override fun toString(): String = "CookieModel(${hashCode()})"

    fun toSensitiveString(): String = "CookieModel(id=$id, name=$name, cookie=$cookie)"
}

@Dao
interface CookieDao {

    @Query("SELECT * FROM cookies")
    suspend fun selectAll(): List<CookieEntity>

    @Query("SELECT * FROM cookies WHERE id = :id")
    suspend fun selectById(id: Long): CookieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookie(cookie: CookieEntity)

    @Query("DELETE FROM cookies WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(version = CookiesDb.VERSION)
object CookiesDb {
    const val NAME = "Cookies"
    const val VERSION = 2
}

@Parcelize
@Table(database = CookiesDb::class, allFields = true, primaryKeyConflict = ConflictAction.REPLACE)
data class CookieModel(@PrimaryKey var id: Long = -1L, var name: String? = null, var cookie: String? = null) :
    BaseModel(), Parcelable {

    override fun toString(): String = "CookieModel(${hashCode()})"

    fun toSensitiveString(): String = "CookieModel(id=$id, name=$name, cookie=$cookie)"
}

fun loadFbCookie(id: Long): CookieModel? =
    (select from CookieModel::class where (CookieModel_Table.id eq id)).querySingle()

fun loadFbCookie(name: String): CookieModel? =
    (select from CookieModel::class where (CookieModel_Table.name eq name)).querySingle()

/**
 * Loads cookies sorted by name
 */
fun loadFbCookiesAsync(callback: (cookies: List<CookieModel>) -> Unit) {
    (select from CookieModel::class).orderBy(CookieModel_Table.name, true).async()
        .queryListResultCallback { _, tResult -> callback(tResult) }.execute()
}

fun loadFbCookiesSync(): List<CookieModel> =
    (select from CookieModel::class).orderBy(CookieModel_Table.name, true).queryList()

// TODO temp method until dbflow supports coroutines
suspend fun loadFbCookiesSuspend(): List<CookieModel> = withContext(Dispatchers.IO) {
    loadFbCookiesSync()
}

inline fun saveFbCookie(cookie: CookieModel, crossinline callback: (() -> Unit) = {}) {
    cookie.async save {
        L.d { "Fb cookie saved" }
        L._d { cookie.toSensitiveString() }
        callback()
    }
}

fun removeCookie(id: Long) {
    loadFbCookie(id)?.async?.delete {
        L.d { "Fb cookie deleted" }
        L._d { id }
    }
}
