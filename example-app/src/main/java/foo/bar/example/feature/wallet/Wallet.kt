package foo.bar.example.feature.wallet

import co.early.fore.kt.core.logging.Logger
import co.early.fore.core.observer.Observable
import co.early.fore.kt.core.observer.ObservableImp
import co.early.persista.PerSista

/**
 * Copyright © 2015-2021 early.co. All rights reserved.
 */

class Wallet(
    private val perSista: PerSista,
    private val logger: Logger,
) : Observable by ObservableImp() {

    var state = WalletState(0)
        private set

    init {
        perSista.read(state) {
            state = it
            notifyObservers()
        }
    }

    fun increaseMobileWallet() {
        if (state.canIncrease()) {
            perSista.write(state.copy(mobileWalletAmount = state.mobileWalletAmount + 1)) {
                logger.i("Increased mobile wallet to:${it.mobileWalletAmount}")
                state = it
                notifyObservers()
            }
        }
    }

    fun decreaseMobileWallet() {
        if (state.canDecrease()) {
            perSista.write(state.copy(mobileWalletAmount = state.mobileWalletAmount - 1)) {
                logger.i("Decreased mobile wallet to:${it.mobileWalletAmount}")
                state = it
                notifyObservers()
            }
        }
    }

    fun resetMobileWallet() {
        perSista.clear(state::class){
            logger.i("Persistent storage cleared")
            state = WalletState(0)
            notifyObservers()
        }
    }
}
