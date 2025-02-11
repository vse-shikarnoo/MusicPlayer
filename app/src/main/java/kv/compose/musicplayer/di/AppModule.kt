package kv.compose.musicplayer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kv.compose.musicplayer.data.remote.DeezerApi
import kv.compose.musicplayer.data.repository.MusicRepositoryImpl
import kv.compose.musicplayer.domain.repository.MusicRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideDeezerApi(okHttpClient: OkHttpClient): DeezerApi {
        return Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMusicRepository(
        api: DeezerApi,
        @ApplicationContext context: Context
    ): MusicRepository {
        return MusicRepositoryImpl(api, context)
    }
}