package com.hskmaster.app.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google Play Billing for in-app purchases.
 *
 * Usage:
 * 1. Initialize with context
 * 2. Call connect() to establish connection
 * 3. Observe isPremium StateFlow for purchase state
 * 4. Call launchPurchaseFlow() to initiate purchase
 */
class BillingManager(
    private val context: Context,
    private val purchaseManager: PurchaseManager
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        // Product ID - must match what you create in Google Play Console
        const val PRODUCT_ID_PREMIUM = "premium_unlock"
    }

    private var billingClient: BillingClient? = null

    private val _isPremium = MutableStateFlow(purchaseManager.isPremium())
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _purchaseInProgress = MutableStateFlow(false)
    val purchaseInProgress: StateFlow<Boolean> = _purchaseInProgress.asStateFlow()

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
    }

    /**
     * Connect to Google Play Billing service
     */
    fun connect() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected successfully")
                    _isConnected.value = true

                    // Query existing purchases
                    queryPurchases()

                    // Query available products
                    queryProductDetails()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _isConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing client disconnected")
                _isConnected.value = false
                // Try to reconnect
                connect()
            }
        })
    }

    /**
     * Query existing purchases to restore premium status
     */
    private fun queryPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            } else {
                Log.e(TAG, "Query purchases failed: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Query product details (price, description) from Play Store
     */
    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
                    _productDetails.value = productDetailsList.first()
                    Log.d(TAG, "Product details loaded: ${productDetailsList.first().name}")
                } else {
                    Log.w(TAG, "No products found. Make sure you've created the product in Play Console.")
                }
            } else {
                Log.e(TAG, "Query product details failed: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Launch the purchase flow for premium unlock
     */
    fun launchPurchaseFlow(activity: Activity): Boolean {
        val productDetails = _productDetails.value

        if (productDetails == null) {
            Log.e(TAG, "Cannot launch purchase: product details not loaded")
            return false
        }

        if (_purchaseInProgress.value) {
            Log.w(TAG, "Purchase already in progress")
            return false
        }

        _purchaseInProgress.value = true

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)

        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Launch billing flow failed: ${billingResult?.debugMessage}")
            _purchaseInProgress.value = false
            return false
        }

        return true
    }

    /**
     * Called when purchase is updated (completed, cancelled, etc.)
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        _purchaseInProgress.value = false

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    processPurchases(purchases)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled the purchase")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Process purchases and grant entitlements
     */
    private fun processPurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.products.contains(PRODUCT_ID_PREMIUM)) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    // Grant premium access
                    grantPremium()

                    // Acknowledge the purchase if not already acknowledged
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    Log.d(TAG, "Purchase is pending")
                    // You might want to show a message that purchase is pending
                }
            }
        }
    }

    /**
     * Acknowledge purchase (required within 3 days or purchase is refunded)
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Grant premium access to user
     */
    private fun grantPremium() {
        purchaseManager.setPremium(true)
        _isPremium.value = true
        Log.d(TAG, "Premium access granted!")
    }

    /**
     * Get formatted price string from product details
     */
    fun getFormattedPrice(): String {
        return _productDetails.value?.oneTimePurchaseOfferDetails?.formattedPrice ?: "£0.99"
    }

    /**
     * Check if billing is ready for purchases
     */
    fun isReady(): Boolean {
        return _isConnected.value && _productDetails.value != null
    }

    /**
     * Disconnect billing client
     */
    fun disconnect() {
        billingClient?.endConnection()
        _isConnected.value = false
    }
}
