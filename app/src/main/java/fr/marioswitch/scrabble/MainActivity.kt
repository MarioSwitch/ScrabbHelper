package fr.marioswitch.scrabble

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.marioswitch.scrabble.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dictionarySelectedFile: String
    private lateinit var dictionarySelectedArray: ArrayList<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //Returns index of element if is found in array, -1 otherwise
        fun indexOfChar(array: ArrayList<Char>, element: Char): Int{
            var i = 0
            for(char in array){
                if(char == element){ return i }
                else{ i++ }
            }
            return -1
        }

        //Converts dictionary file to ArrayList<String>
        fun convertDictionaryToArrayList(dictionary: String, context: Context): ArrayList<String>{
            val array = ArrayList<String>()
            try {
                context.assets.open(dictionary).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            array.add(line!!)
                        }
                    }
                }
            } catch (e: IOException) {
                Toast.makeText(this, e.toString(),Toast.LENGTH_LONG).show()
            }
            return array
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
                    0 -> "ods8.txt"
                    1 -> "csw19.txt"
                    2 -> "nwl2020.txt"
                    else -> "ods8.txt"
                }
                dictionarySelectedArray = convertDictionaryToArrayList(dictionarySelectedFile, this@MainActivity)
                binding.dictionaryWords.text = getString(R.string.dictionary_words, dictionarySelectedArray.size)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        //Rest of the code
        binding.searchClear.setOnClickListener {
            binding.searchInput.text.clear()
        }

        binding.searchButton.setOnClickListener {
            val search = binding.searchInput.text
            val mode = binding.searchModeSelect.checkedRadioButtonId
            var modeText = try {
                findViewById<RadioButton>(mode).text as String
            }catch (e: Exception){
                "error_mode"
            }
            if(search.isEmpty()){
                modeText = "error_search"
            }
            try {
                search.toString().toRegex()
            }catch (e: Exception){
                modeText = "error_search"
            }
            when(modeText){
                "error_mode" -> {
                    binding.resultTitle.text = getString(R.string.error_mode)
                    binding.resultContent.text = ""
                }
                "error_search" -> {
                    binding.resultTitle.text = getString(R.string.error_search)
                    binding.resultContent.text = ""
                }
                getString(R.string.search_mode_word) -> {
                    //Validity
                    if(listAllMatches("^$search$".toRegex(RegexOption.IGNORE_CASE), dictionarySelectedArray).size>0){
                        binding.resultTitle.text = getString(R.string.result_title_valid, search)
                    }else{
                        binding.resultTitle.text = getString(R.string.result_title_invalid, search)
                    }
                    binding.resultContent.text = ""
                }
                getString(R.string.search_mode_list) -> {
                    //Regex filter
                    val wordList = listAllMatches("$search".toRegex(RegexOption.IGNORE_CASE), dictionarySelectedArray)
                    val wordCount = wordList.size
                    binding.resultTitle.text = getString(R.string.result_title_list, wordCount, search)
                    binding.resultContent.text = wordList.joinToString(", ")
                }
                getString(R.string.search_mode_anagrams) -> {
                    //Anagrams
                    val blanks = search.count{it == '.'}
                    var searchNoBlanks = ""
                    for(letter in search){
                        if(letter != '.'){ searchNoBlanks += letter }
                    }
                    var regexPart = "["
                    for(letter in search){
                        if(letter != '.'){ regexPart += "$letter" }
                    }
                    regexPart += "]*"
                    var regex = "^"
                    regex += regexPart
                    for(i in 1..blanks){
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
                    val wordList = ArrayList<String>()
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
                    val wordCount = wordList.size
                    binding.resultTitle.text = getString(R.string.result_title_anagrams, wordCount, search)
                    var resultText = ""
                    for(i in search.length downTo 2){
                        val wordListLetter = listAllMatches("^.{$i}$".toRegex(RegexOption.IGNORE_CASE), wordList)
                        if(wordListLetter.size > 0){
                            resultText += getString(R.string.result_content_anagrams, i, wordListLetter.size)
                            resultText += "\n"
                            resultText += wordListLetter.joinToString(", ")
                            resultText += "\n\n"
                        }
                    }
                    binding.resultContent.text = resultText
                }
            }
        }
    }
}