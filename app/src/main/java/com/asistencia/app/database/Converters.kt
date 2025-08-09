package com.asistencia.app.database

import androidx.room.TypeConverter

class Converters {
    
    @TypeConverter
    fun fromTipoHorario(tipoHorario: TipoHorario): String {
        return tipoHorario.name
    }
    
    @TypeConverter
    fun toTipoHorario(tipoHorario: String): TipoHorario {
        return TipoHorario.valueOf(tipoHorario)
    }
    
    @TypeConverter
    fun fromTipoEvento(tipoEvento: TipoEvento): String {
        return tipoEvento.name
    }
    
    @TypeConverter
    fun toTipoEvento(tipoEvento: String): TipoEvento {
        return TipoEvento.valueOf(tipoEvento)
    }
    
    @TypeConverter
    fun fromModoLectura(modoLectura: ModoLectura): String {
        return modoLectura.name
    }
    
    @TypeConverter
    fun toModoLectura(modoLectura: String): ModoLectura {
        return ModoLectura.valueOf(modoLectura)
    }
    
    @TypeConverter
    fun fromEstadoSync(estadoSync: EstadoSync): String {
        return estadoSync.name
    }
    
    @TypeConverter
    fun toEstadoSync(estadoSync: String): EstadoSync {
        return EstadoSync.valueOf(estadoSync)
    }
    
    @TypeConverter
    fun fromModoOperacion(modoOperacion: ModoOperacion): String {
        return modoOperacion.name
    }
    
    @TypeConverter
    fun toModoOperacion(modoOperacion: String): ModoOperacion {
        return ModoOperacion.valueOf(modoOperacion)
    }
}