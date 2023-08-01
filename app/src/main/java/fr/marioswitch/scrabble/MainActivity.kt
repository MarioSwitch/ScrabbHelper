package fr.marioswitch.scrabble

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import fr.marioswitch.scrabble.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

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

        //TODO: ability to choose which dictionary to use
        val dictionariesFiles = ArrayList<String>()
        val dictionariesNames = ArrayList<String>()
        dictionariesFiles.add("ods8.txt")
        dictionariesNames.add("\uD83C\uDDEB\uD83C\uDDF7 Unofficial dictionary (2021)")
        dictionariesFiles.add("csw19.txt")
        dictionariesNames.add("\uD83C\uDDEC\uD83C\uDDE7 CSW19")
        dictionariesFiles.add("nwl2020.txt")
        dictionariesNames.add("\uD83C\uDDFA\uD83C\uDDF8 NWL2020")
        val dictionarySelected = 0
        val dictionarySelectedFile = dictionariesFiles.elementAt(dictionarySelected)
        val dictionarySelectedName = dictionariesNames.elementAt(dictionarySelected)
        val dictionarySelectedArray = convertDictionaryToArrayList(dictionarySelectedFile, this@MainActivity)
        val totalWords = dictionarySelectedArray.size

        binding.dictionary.text = getString(R.string.dictionary, dictionarySelectedName, totalWords)

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

        binding.searchButton.setOnClickListener{
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
                    binding.resultContent.text = wordList.toString()
                }
                getString(R.string.search_mode_anagrams) -> {
                    //Anagrams
                    //TODO: filter wordList to check if words are actually anagrams
                    val searchSize = search.length
                    var regex = ""
                    regex += "^["
                    for(letter in search){
                        regex += "$letter"
                    }
                    regex += "]{0,$searchSize}$"
                    val wordList = listAllMatches(regex.toRegex(RegexOption.IGNORE_CASE), dictionarySelectedArray)
                    val wordCount = wordList.size
                    binding.resultTitle.text = getString(R.string.result_title_anagrams, wordCount, search)
                    binding.resultContent.text = wordList.toString()
                }
            }
        }
    }
}