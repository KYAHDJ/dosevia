package com.dosevia.app

import android.app.Activity
import android.content.Context
import android.util.Log
import java.security.MessageDigest
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val BILLING_TAG = "DoseviaBilling"

class BillingManager(
    context: Context,
) : PurchasesUpdatedListener {

    data class PurchaseEvent(
        val productIds: Set<String> = emptySet(),
        val timestamp: Long = 0L
    )

    private val appContext = context.applicationContext

    private val _tierFromBilling = MutableStateFlow<UserTier?>(null)
    val tierFromBilling: StateFlow<UserTier?> = _tierFromBilling

    private val _lifetimeDetails = MutableStateFlow<ProductDetails?>(null)
    val lifetimeDetails: StateFlow<ProductDetails?> = _lifetimeDetails

    private val _proDetails = MutableStateFlow<ProductDetails?>(null)
    val proDetails: StateFlow<ProductDetails?> = _proDetails

    private val _purchaseEvent = MutableStateFlow(PurchaseEvent())
    val purchaseEvent: StateFlow<PurchaseEvent> = _purchaseEvent

    private val _billingReady = MutableStateFlow(false)
    val billingReady: StateFlow<Boolean> = _billingReady

    @Volatile
    private var currentAccountHash: String? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    fun start(accountEmail: String? = null) {
        currentAccountHash = accountEmail.toObfuscatedAccountId()
        _billingReady.value = false

        if (billingClient.isReady) {
            Log.d(BILLING_TAG, "Billing already ready. Refreshing products and purchases.")
            queryProducts()
            restorePurchases()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(BILLING_TAG, "Billing connected")
                    queryProducts()
                    restorePurchases()
                } else {
                    Log.w(BILLING_TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _billingReady.value = true
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(BILLING_TAG, "Billing disconnected")
                _billingReady.value = false
            }
        })
    }

    fun end() {
        try {
            billingClient.endConnection()
        } catch (_: Exception) {
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            Log.d(BILLING_TAG, "restorePurchases skipped because billing is not ready")
            return
        }

        _billingReady.value = false

        val requiredAccountHash = currentAccountHash
        if (requiredAccountHash.isNullOrBlank()) {
            Log.d(BILLING_TAG, "No signed-in app account; forcing FREE tier")
            applyResolvedTier(UserTier.FREE)
            return
        }

        queryActivePurchases(BillingClient.ProductType.INAPP) { inAppPurchases ->
            queryActivePurchases(BillingClient.ProductType.SUBS) { subPurchases ->
                handlePurchases(inAppPurchases + subPurchases)
            }
        }
    }

    private fun queryActivePurchases(
        productType: String,
        onComplete: (List<Purchase>) -> Unit,
    ) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                onComplete(purchases ?: emptyList())
            } else {
                Log.w(BILLING_TAG, "Restore failed for $productType: ${result.debugMessage}")
                onComplete(emptyList())
            }
        }
    }

    private fun queryProducts() {
        if (!billingClient.isReady) return

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingProducts.LIFETIME_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(inAppParams) { result, detailsList ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(BILLING_TAG, "INAPP product query failed: ${result.debugMessage}")
                return@queryProductDetailsAsync
            }
            _lifetimeDetails.value =
                detailsList.firstOrNull { it.productId == BillingProducts.LIFETIME_PRODUCT_ID }
        }

        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingProducts.PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { result, detailsList ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(BILLING_TAG, "SUBS product query failed: ${result.debugMessage}")
                return@queryProductDetailsAsync
            }
            _proDetails.value =
                detailsList.firstOrNull { it.productId == BillingProducts.PRO_PRODUCT_ID }
        }
    }

    fun launchPurchase(activity: Activity, productId: String, accountEmail: String?) {
        val accountHash = accountEmail.toObfuscatedAccountId()
        if (accountHash.isNullOrBlank()) {
            Log.w(BILLING_TAG, "Blocked purchase because no signed-in app account is available")
            return
        }

        currentAccountHash = accountHash

        if (!billingClient.isReady) {
            Log.w(BILLING_TAG, "Billing not ready yet; reconnecting first")
            start(accountEmail)
            return
        }

        val details = when (productId) {
            BillingProducts.LIFETIME_PRODUCT_ID -> _lifetimeDetails.value
            BillingProducts.PRO_PRODUCT_ID -> _proDetails.value
            else -> null
        }

        if (details == null) {
            Log.w(BILLING_TAG, "Missing ProductDetails for $productId; re-querying products")
            queryProducts()
            return
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (productId == BillingProducts.PRO_PRODUCT_ID) {
            val offerToken = details.subscriptionOfferDetails
                ?.firstOrNull {
                    it.offerToken.isNotBlank() &&
                            it.pricingPhases.pricingPhaseList.isNotEmpty()
                }
                ?.offerToken

            if (offerToken.isNullOrBlank()) {
                Log.w(BILLING_TAG, "Missing offer token for subscription ${details.productId}")
                queryProducts()
                return
            }

            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setObfuscatedAccountId(accountHash)
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            acknowledgePurchasesIfNeeded(purchases)

            val purchasedIds = purchases
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .flatMap { it.products }
                .toSet()

            if (purchasedIds.isNotEmpty()) {
                _purchaseEvent.value = PurchaseEvent(
                    productIds = purchasedIds,
                    timestamp = System.currentTimeMillis()
                )
            }

            restorePurchases()
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(BILLING_TAG, "User canceled purchase")
            restorePurchases()
        } else {
            Log.w(BILLING_TAG, "Purchase failed: ${billingResult.debugMessage}")
            restorePurchases()
        }
    }

    private fun acknowledgePurchasesIfNeeded(purchases: List<Purchase>) {
        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { purchase ->
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(params) { result ->
                    Log.d(
                        BILLING_TAG,
                        "Acknowledge: ${result.responseCode} ${result.debugMessage}"
                    )
                }
            }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        acknowledgePurchasesIfNeeded(purchases)

        val requiredAccountHash = currentAccountHash
        val activePurchases = purchases.filter { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.matchesBoundAccount(requiredAccountHash)
        }

        val ownsLifetime = activePurchases.any {
            it.products.contains(BillingProducts.LIFETIME_PRODUCT_ID)
        }

        val ownsPro = activePurchases.any {
            it.products.contains(BillingProducts.PRO_PRODUCT_ID)
        }

        val resolvedTier = when {
            ownsLifetime -> UserTier.LIFETIME
            ownsPro -> UserTier.PRO
            else -> UserTier.FREE
        }

        Log.d(
            BILLING_TAG,
            "Resolved tier from billing = $resolvedTier, " +
                    "activePurchases=${activePurchases.map { it.products }}"
        )

        applyResolvedTier(resolvedTier)
    }

    private fun applyResolvedTier(tier: UserTier) {
        _tierFromBilling.value = tier
        PremiumAccess.writeTier(appContext, tier)
        _billingReady.value = true
    }

    private fun Purchase.matchesBoundAccount(requiredAccountHash: String?): Boolean {
        if (requiredAccountHash.isNullOrBlank()) return false

        val purchaseHash = try {
            accountIdentifiers?.obfuscatedAccountId
        } catch (_: Exception) {
            null
        }

        if (purchaseHash.isNullOrBlank()) {
            Log.w(BILLING_TAG, "Ignoring purchase without matching obfuscated account binding")
            return false
        }

        return purchaseHash == requiredAccountHash
    }

    private fun String?.toObfuscatedAccountId(): String? {
        val normalized = this?.trim()?.lowercase().orEmpty()
        if (normalized.isBlank()) return null

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray())

        return digest.joinToString("") { "%02x".format(it) }.take(64)
    }
}