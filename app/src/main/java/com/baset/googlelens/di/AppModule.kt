package com.baset.googlelens.di

import android.content.Context
import com.baset.googlelens.util.ImageLabeling
import com.baset.googlelens.util.ModuleInstaller
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideImageLabeling(@ApplicationContext context: Context): ImageLabeling {
        return ImageLabeling(context)
    }

    @Provides
    @Singleton
    fun provideModuleInstaller(@ApplicationContext context: Context): ModuleInstaller {
        return ModuleInstaller(context)
    }
}