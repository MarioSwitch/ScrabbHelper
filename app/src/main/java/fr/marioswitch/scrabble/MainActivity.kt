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
        val dictionarySelectedFile = "ods8.txt"
        val dictionarySelected = convertDictionaryToArrayList(dictionarySelectedFile, this@MainActivity)

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
            when(findViewById<RadioButton>(mode).text){
                getString(R.string.search_mode_word) -> {
                    //Validity
                    if(listAllMatches("^$search$".toRegex(RegexOption.IGNORE_CASE), dictionarySelected).size>0){
                        binding.resultTitle.text = getString(R.string.result_title_valid, search)
                    }else{
                        binding.resultTitle.text = getString(R.string.result_title_invalid, search)
                    }
                    binding.resultContent.text = ""
                }
                getString(R.string.search_mode_list) -> {
                    //Regex filter
                    val wordList = listAllMatches("$search".toRegex(RegexOption.IGNORE_CASE), dictionarySelected)
                    val wordCount = wordList.size
                    binding.resultTitle.text = getString(R.string.result_title_list, wordCount, search)
                    binding.resultContent.text = wordList.toString()
                }
                getString(R.string.search_mode_anagrams) -> {
                    //Anagrams
                    //TODO: this code doesn't take into account words with 2+ same letters
                    var regex = "^"
                    for(letter in search){
                        regex += "(?!.*$letter.*$letter)"
                    }
                    regex += "["
                    for(letter in search){
                        regex += "$letter"
                    }
                    regex += "]*$"
                    val wordList = listAllMatches(regex.toRegex(RegexOption.IGNORE_CASE), dictionarySelected)
                    val wordCount = wordList.size
                    binding.resultTitle.text = getString(R.string.result_title_anagrams, wordCount, search)
                    binding.resultContent.text = wordList.toString()
                }
            }
        }
    }
}