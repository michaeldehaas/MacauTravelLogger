package com.mikec.macautravellogger.di

import android.content.Context
import androidx.room.Room
import com.mikec.macautravellogger.data.local.TravelDatabase
import com.mikec.macautravellogger.data.local.TravelEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TravelDatabase =
        Room.databaseBuilder(
            context,
            TravelDatabase::class.java,
            TravelDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideDao(database: TravelDatabase): TravelEntryDao = database.travelEntryDao()
}
