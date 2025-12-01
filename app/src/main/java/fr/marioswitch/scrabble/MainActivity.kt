package fr.marioswitch.scrabble

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.TypefaceSpan
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import fr.marioswitch.scrabble.databinding.ActivityMainBinding
import java.text.DecimalFormat
import java.text.Normalizer
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dictionarySelectedFile: String
    private lateinit var dictionarySelectedArray: ArrayList<String>
    private lateinit var dictionarySelectedSet: HashSet<String>
    private lateinit var definitionsFile: String
    private lateinit var definitionsMap: Map<String, String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initializes save
        val save = getSharedPreferences("fr.marioswitch.scrabble", MODE_PRIVATE)

        //Returns index of element if is found in array, -1 otherwise
        fun indexOfChar(array: ArrayList<Char>, element: Char): Int{
            var i = 0
            for(char in array){
                if(char == element){ return i }
                else{ i++ }
            }
            return -1
        }

        //Applies thousand separators
        fun applyThousandSeparator(value:Int):String{
            val formatter = DecimalFormat("#,##0")
            formatter.decimalFormatSymbols = formatter.decimalFormatSymbols.apply { groupingSeparator = this@MainActivity.getString(R.string.thousand_separator).toCharArray()[0] }
            return formatter.format(value)
        }

        //Remove accents for redirects
        fun String.removeAccents() = Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "")

        //Get definitions of a word
        fun getDefinitions(word: String, definitionsPath: String, context: Context, redirect: Boolean = false): String {
            var definition = definitionsMap[word.uppercase()]
            if(definition == null) return getString(R.string.result_definitions_not_found, word)
            if(definition == "") return getString(R.string.result_definitions_error, word)
            if(definition.startsWith("ERROR_")) return getString(R.string.result_definitions_error, word)

            definition = definition.replace("\\n", "\n\n")

            if(redirect) return "\n\n[$word]\n\n$definition"

            val conjugation = Regex("du verbe[  ]+(.*)\\.").find(definition)
            if(conjugation != null) definition += getDefinitions(conjugation.groupValues[1].removeAccents(), definitionsPath, context, true)

            val feminine = Regex("Féminin de[  ]+([^ .]*)\\.", RegexOption.IGNORE_CASE).find(definition)
            if(feminine != null) definition += getDefinitions(feminine.groupValues[1].removeAccents(), definitionsPath, context, true)

            val plural = Regex("Pluriel de[  ]+([^ .]*)\\.", RegexOption.IGNORE_CASE).find(definition)
            if(plural != null) definition += getDefinitions(plural.groupValues[1].removeAccents(), definitionsPath, context, true)

            val variant = Regex("Variante (orthographique )?de[  ]+([^ .]*)\\.", RegexOption.IGNORE_CASE).find(definition)
            if(variant != null) definition += getDefinitions(variant.groupValues[variant.groupValues.size-1].removeAccents(), definitionsPath, context, true)

            val credits = when {
                "fr" in definitionsPath -> getString(R.string.result_definitions_credits_fr)
                "en" in definitionsPath -> getString(R.string.result_definitions_credits_en)
                else -> ""
            }

            definition += "\n\n" + credits
            return definition
        }

        //Lists all strings from dictionary matching regexp
        fun listAllMatches(regexp: Regex, dictionary: ArrayList<String>): ArrayList<String> {
            val matchingWords = ArrayList<String>()
            for(word in dictionary){
                val matchResult = regexp.find(word)
                if (matchResult != null) {
                    matchingWords.add(word)
                }
            }
            return matchingWords
        }

        //Dictionaries
        val dictionarySpinner = binding.dictionarySpinner
        ArrayAdapter.createFromResource(this, R.array.dictionary_list, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dictionarySpinner.adapter = adapter
        }
        dictionarySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                dictionarySelectedFile = when (position) {
                    0 -> "ods9.txt"
                    1 -> "ods8.txt"
                    2 -> "csw24.txt"
                    3 -> "csw21.txt"
                    4 -> "csw19.txt"
                    5 -> "nwl2023.txt"
                    6 -> "nwl2020.txt"
                    else -> "ods9.txt"
                }

                binding.dictionaryWords.text = ""
                binding.resultTitle.setTextAppearance(R.style.result_title)
                binding.resultTitle.text = getString(R.string.loading_title)
                binding.resultContent.text = getString(R.string.loading_content, getString(R.string.app_name))

                CoroutineScope(Dispatchers.IO).launch {
                    dictionarySelectedArray = ArrayList(this@MainActivity.assets.open(dictionarySelectedFile).bufferedReader().useLines { it.toList() })
                    dictionarySelectedSet = dictionarySelectedArray.toHashSet()

                    definitionsFile = when (position) {
                        in 0..1 -> "definitions_fr.json"
                        in 2..6 -> "definitions_en.json"
                        else -> "definitions_fr.json"
                    }
                    val map = HashMap<String, String>(1000000)
                    this@MainActivity.assets.open(definitionsFile).bufferedReader().useLines { lines ->
                        for (line in lines) {
                            val match = Regex("\t\"([^\"]+)\": \"(.*)\"", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)).find(line)
                            if(match == null) continue
                            map[match.groupValues[1]] = match.groupValues[2]
                        }
                    }
                    definitionsMap = map

                    withContext(Dispatchers.Main){
                        val dictionarySelectedSize = applyThousandSeparator(dictionarySelectedArray.size)
                        binding.dictionaryWords.text = getString(R.string.dictionary_words, dictionarySelectedSize)
                        binding.resultTitle.text = ""
                        binding.resultContent.text = ""
                        save.edit { putInt("dictionary", position) } //Saves dictionary selected
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        //Rest of the code
        val maxResults = 10000
        val maxAnagrams = 1000
        dictionarySpinner.setSelection(save.getInt("dictionary",0)) //Selects dictionary based on user save (or 0 (ODS9) if no save)
        binding.searchModeSelect.check(R.id.search_mode_word) //Selects Validity mode by default

        binding.searchClear.setOnClickListener {
            binding.searchInput.text.clear()
            binding.resultTitle.text = ""
            binding.resultContent.text = ""
        }

        binding.searchButton.setOnClickListener {
            val search = binding.searchInput.text.toString()
            var mode = findViewById<RadioButton>(binding.searchModeSelect.checkedRadioButtonId).text.toString()

            if(search.isEmpty()){
                mode = "error_search"
            }
            try {
                search.toRegex()
            }catch (_: Exception){
                mode = "error_search"
            }
            if(mode == getString(R.string.search_mode_word) && !search.matches("^[a-zA-Z]*".toRegex())){
                mode = "error_validity"
            }
            if(mode == getString(R.string.search_mode_anagrams) && !search.matches("^[a-zA-Z.]*".toRegex())){
                mode = "error_anagrams"
            }
            when(mode){
                "error_search" -> {
                    binding.resultTitle.setTextAppearance(R.style.result_title)
                    binding.resultTitle.text = getString(R.string.error_search)
                    binding.resultContent.text = ""
                }
                "error_validity" -> {
                    binding.resultTitle.setTextAppearance(R.style.result_title)
                    binding.resultTitle.text = getString(R.string.error_search)
                    binding.resultContent.text = getString(R.string.error_validity)
                }
                "error_anagrams" -> {
                    binding.resultTitle.setTextAppearance(R.style.result_title)
                    binding.resultTitle.text = getString(R.string.error_search)
                    binding.resultContent.text = getString(R.string.error_anagrams)
                }
                getString(R.string.search_mode_word) -> {
                    //Validity
                    if(search.uppercase() in dictionarySelectedSet){
                        binding.resultTitle.setTextAppearance(R.style.result_title_green)
                        binding.resultTitle.text = getString(R.string.result_title_valid, search)
                        binding.resultContent.text = getDefinitions(search, definitionsFile, this@MainActivity)
                    }else{
                        binding.resultTitle.setTextAppearance(R.style.result_title_red)
                        binding.resultTitle.text = getString(R.string.result_title_invalid, search)
                        binding.resultContent.text = ""
                    }
                }
                getString(R.string.search_mode_list) -> {
                    //RegEx filter
                    val wordList = listAllMatches(search.toRegex(RegexOption.IGNORE_CASE), dictionarySelectedArray)
                    val wordCount = applyThousandSeparator(wordList.size)
                    val string1 = getString(R.string.result_title_list, wordCount) + " "
                    val string2 = search
                    binding.resultTitle.setTextAppearance(R.style.result_title)
                    val spannable = SpannableStringBuilder()
                    spannable.append(string1)
                    spannable.append(string2)
                    spannable.setSpan(TypefaceSpan("monospace"), string1.length, string1.length+string2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    binding.resultTitle.text = spannable
                    if(wordList.size<=maxResults){
                        binding.resultContent.text = wordList.joinToString(", ")
                    }
                    else{
                        binding.resultContent.text = getString(R.string.too_many_results, applyThousandSeparator(maxResults))
                    }
                }
                getString(R.string.search_mode_anagrams) -> {
                    //Anagrams
                    val blanks = search.count{it == '.'}
                    var regex: String
                    var wordList = ArrayList<String>()
                    if(blanks == search.length){
                        regex = "^.{0,$blanks}$"
                        wordList = listAllMatches(regex.toRegex(RegexOption.IGNORE_CASE), dictionarySelectedArray)
                    }else{
                        var searchNoBlanks = ""
                        for(letter in search){
                            if(letter != '.'){ searchNoBlanks += letter }
                        }
                        var regexPart = "["
                        for(letter in search){
                            if(letter != '.'){ regexPart += "$letter" }
                        }
                        regexPart += "]*"
                        regex = "^"
                        regex += regexPart
                        repeat(blanks){
                            regex += "."
                            regex += regexPart
                        }
                        regex += "$"
                        val filteredList = listAllMatches(regex.toRegex(RegexOption.IGNORE_CASE), dictionarySelectedArray)
                        val searchUpper = searchNoBlanks.uppercase()
                        val uniqueLetters = ArrayList<Char>()
                        val uniqueSearch = ArrayList<Int>()
                        val uniqueWord = ArrayList<Int>()
                        for(letter in searchUpper){
                            val index = indexOfChar(uniqueLetters, letter)
                            if(index >= 0){
                                uniqueSearch[index]++
                            }else{
                                uniqueLetters.add(letter)
                                uniqueSearch.add(1)
                            }
                        }
                        uniqueLetters.add('.')
                        uniqueSearch.add(blanks)
                        var addWord:Boolean
                        for(word in filteredList){
                            addWord = true
                            if(word.length > search.length){ addWord = false }
                            uniqueWord.clear()
                            for(char in uniqueLetters){
                                uniqueWord.add(word.count{it == char})
                            }
                            var blanksInWord = word.length - uniqueWord.sum()
                            for(i in 0 until uniqueLetters.size-1){
                                while(uniqueWord[i] > uniqueSearch[i]){
                                    uniqueWord[i]--
                                    blanksInWord++
                                }
                            }
                            if(blanksInWord > uniqueSearch[uniqueSearch.size-1]){ addWord = false }
                            if(addWord){ wordList.add(word) }
                        }
                    }
                    val wordCount = applyThousandSeparator(wordList.size)
                    binding.resultTitle.setTextAppearance(R.style.result_title)
                    binding.resultTitle.text = getString(R.string.result_title_anagrams, wordCount, search)
                    var resultText = ""
                    var anagramsDisplayed = 0
                    for(i in search.length downTo 2){
                        val wordListLetter = listAllMatches("^.{$i}$".toRegex(RegexOption.IGNORE_CASE), wordList)
                        anagramsDisplayed += wordListLetter.size
                        if(wordListLetter.isNotEmpty()){
                            resultText += if(i == search.length){
                                getString(R.string.result_content_anagrams_perfect, i, applyThousandSeparator(wordListLetter.size))
                            }else{
                                getString(R.string.result_content_anagrams, i, applyThousandSeparator(wordListLetter.size))
                            }
                            resultText += "\n"
                            resultText += wordListLetter.joinToString(", ")
                            resultText += "\n\n"
                        }
                        if(anagramsDisplayed >= maxAnagrams){
                            resultText += getString(R.string.too_many_anagrams, applyThousandSeparator(maxAnagrams), applyThousandSeparator(anagramsDisplayed))
                            break
                        }
                    }
                    binding.resultContent.text = resultText
                }
            }
        }
    }
}