package com.mikyegresl.valostat.di

import com.mikyegresl.valostat.base.network.config.NetworkConfigProvider
import com.mikyegresl.valostat.network.NetworkFactory
import com.mikyegresl.valostat.provider.NetworkConfigProviderImpl
import org.koin.dsl.module

val networkModule = module {

    single<NetworkConfigProvider> { NetworkConfigProviderImpl(get(), get()) }

    single {
        NetworkFactory(
            get<NetworkConfigProvider>().getNetworkConfig()
        )
    }

    single { get<NetworkFactory>().agentsRemoteDataSource }
    single { get<NetworkFactory>().weaponsRemoteDataSource }
    single { get<NetworkFactory>().videosRemoteDataSource }
    single { get<NetworkFactory>().newsRemoteDataSource }
}