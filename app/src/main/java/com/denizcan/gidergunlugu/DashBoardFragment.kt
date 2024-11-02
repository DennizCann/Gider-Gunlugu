package com.denizcan.gidergunlugu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.denizcan.gidergunlugu.databinding.FragmentDashBoardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashBoardFragment : Fragment() {

    private var _binding: FragmentDashBoardBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currencyService: CurrencyService = CurrencyService()

    // Veritabanından alınan orijinal TL değerleri
    private var originalIncome: Double = 0.0
    private var originalExpenses: Double = 0.0
    private var originalRemaining: Double = 0.0

    // Hedef para birimine çevrilen değerler
    private var convertedIncome: Double = 0.0
    private var convertedExpenses: Double = 0.0
    private var convertedRemaining: Double = 0.0

    private var targetCurrencyRate: Double = 1.0 // Döviz kuru oranını tutan değişken
    private var isInUSD: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashBoardBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true) // Menü eklenmesini sağlar
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currencyService = CurrencyService()

        // Kullanıcı profilini yükle
        loadUserProfile()

        // Gelir ve gider bilgilerini yükle
        loadIncomeFromFirestore()
        loadExpensesFromFirestore()

        // Döviz geçiş butonu işlevi
        binding.currencyToggleButton.setOnClickListener {
            loadCurrencyRatesAndUpdate() // Butona tıklandığında döviz kurunu yükle ve güncelle
        }
    }

    private fun loadCurrencyRatesAndUpdate() {
        currencyService.getCurrencyRates("2f829a3e873fad29e950d9ce88b265ff") { rate ->
            rate?.let {
                targetCurrencyRate = it // TRY döviz kuru USD bazında burada kaydedilir
                updateDashboardWithCurrency() // Güncellenmiş kur oranı ile dashboard'u güncelle
            } ?: run {
                Toast.makeText(requireContext(), "Döviz kuru alınamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("userName")
                        val job = document.getString("job")
                        val dob = document.getString("dob")

                        // Yaşı hesaplama
                        val age = calculateAge(dob)

                        // Profil bilgilerini ekranda gösterme
                        binding.profileInfoTextView.text = "Ad: $userName\nMeslek: $job\nYaş: $age"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Profil bilgisi alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun calculateAge(dob: String?): Int {
        if (dob.isNullOrEmpty()) {
            return 0
        }

        val dobParts = dob.split("-")
        if (dobParts.size == 3) {
            val year = dobParts[0].toIntOrNull() ?: return 0
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            return currentYear - year
        }
        return 0
    }

    private fun loadIncomeFromFirestore() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId)
                .collection("incomes")
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val incomeString = document.getString("income")
                        originalIncome = incomeString?.toDoubleOrNull() ?: 0.0
                    }
                    calculateRemaining()
                    updateDashboardWithCurrency()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gelir alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadExpensesFromFirestore() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId)
                .collection("expenses")
                .get()
                .addOnSuccessListener { documents ->
                    originalExpenses = 0.0
                    for (document in documents) {
                        val amountString = document.getString("amount")
                        val amount = amountString?.toDoubleOrNull() ?: 0.0
                        originalExpenses += amount
                    }
                    calculateRemaining()
                    updateDashboardWithCurrency()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Giderler alınamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun calculateRemaining() {
        originalRemaining = originalIncome - originalExpenses
    }

    private fun updateDashboardWithCurrency() {
        isInUSD = !isInUSD
        Log.d("CurrencyConversion", "Döviz kuru: $targetCurrencyRate, USD Modu: $isInUSD")

        val convertedIncome = if (isInUSD) convertCurrency(originalIncome, targetCurrencyRate) else originalIncome
        val convertedExpenses = if (isInUSD) convertCurrency(originalExpenses, targetCurrencyRate) else originalExpenses
        val convertedRemaining = if (isInUSD) convertCurrency(originalRemaining, targetCurrencyRate) else originalRemaining

        // Virgülden sonra en fazla 4 basamak gösterilecek şekilde formatlama
        binding.incomeTextView.text = "Gelir: ${"%.4f".format(convertedIncome)} ${if (isInUSD) "USD" else "TRY"}"
        binding.expenseTextView.text = "Gider: ${"%.4f".format(convertedExpenses)} ${if (isInUSD) "USD" else "TRY"}"
        binding.totalTextView.text = "Kalan: ${"%.4f".format(convertedRemaining)} ${if (isInUSD) "USD" else "TRY"}"
    }

    private fun convertCurrency(amount: Double, targetRate: Double): Double {
        return amount / targetRate
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_dashboard, menu) // Menü XML dosyasını bağlıyoruz
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_income -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, AddIncomeFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            R.id.action_add_expense -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, AddExpenseFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            R.id.action_daily_expense -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, DailyExpenseFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            R.id.action_edit_profile -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ProfileFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            R.id.action_logout -> {
                auth.signOut()
                Toast.makeText(requireContext(), "Çıkış Yapıldı", Toast.LENGTH_SHORT).show()

                // LoginFragment'e geçiş yap
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, LoginFragment())
                    .addToBackStack(null)
                    .commit()

                true
            }


            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
