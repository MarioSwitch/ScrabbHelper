package fr.marioswitch.scrabble

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import fr.marioswitch.scrabble.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //TODO: ability to choose which dictionary to use
        val dictionarySelected = "ods8.txt"

        fun listAllMatches(regexp: Regex, dictionary: String, context: Context): ArrayList<String> {
            val matchingWords = ArrayList<String>()

            try {
                context.assets.open(dictionary).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val matchResult = regexp.find(line!!)
                            if (matchResult != null) {
                                matchingWords.add(line!!)
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Toast.makeText(this, e.toString(),Toast.LENGTH_LONG).show()
            }
            return matchingWords
        }

        binding.searchButton.setOnClickListener{
            val search = binding.searchInput.text
            val mode = binding.searchModeSelect.checkedRadioButtonId
            val modeText = findViewById<RadioButton>(mode).text
            when(modeText){
                getString(R.string.search_mode_word) -> {
                    //Validity
                    if(listAllMatches("^$search$".toRegex(RegexOption.IGNORE_CASE), dictionarySelected, this).size>0){
                        binding.resultTitle.text = getString(R.string.result_title_valid, search)
                    }else{
                        binding.resultTitle.text = getString(R.string.result_title_invalid, search)
                    }
                    binding.resultContent.text = ""
                }
                getString(R.string.search_mode_list) -> {
                    //Words starting with
                    val wordList = listAllMatches("$search".toRegex(RegexOption.IGNORE_CASE), dictionarySelected, this)
                    val wordCount = wordList.size
                    binding.resultTitle.text = getString(R.string.result_title_list, wordCount, search)
                    binding.resultContent.text = wordList.toString()
                }
            }
        }
    }
}