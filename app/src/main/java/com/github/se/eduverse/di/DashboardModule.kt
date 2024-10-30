package com.github.se.eduverse.di

import com.github.se.eduverse.repository.DashboardRepository
import com.github.se.eduverse.repository.DashboardRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DashboardModule {
  @Provides @Singleton fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

  @Provides
  @Singleton
  fun provideDashboardRepository(firestore: FirebaseFirestore): DashboardRepository =
      DashboardRepositoryImpl(firestore)
}
