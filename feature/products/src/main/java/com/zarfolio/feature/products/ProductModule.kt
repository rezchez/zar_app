package com.zarfolio.feature.products

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductModule {
    @Binds @Singleton abstract fun bindProductRepository(impl: ContractPendingProductRepository): ProductRepository
}
