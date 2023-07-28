package fr.marioswitch.scrabble

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioButton
import fr.marioswitch.scrabble.databinding.ActivityMainBinding
import org.intellij.lang.annotations.RegExp

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        fun listAllMatches(regexp: Regex): Array<String>{
            //TODO
            return arrayOf("Test", regexp.toString(), "Test2")
        }
        fun countAllMatches(regexp: Regex): Int{
            //TODO
            return regexp.toString().length
        }

        binding.searchButton.setOnClickListener{
            val search = binding.searchInput.text
            val mode = binding.searchModeSelect.checkedRadioButtonId
            val modeText = findViewById<RadioButton>(mode).text
            when(modeText){
                getString(R.string.search_mode_validity) -> {
                    //Validity
                    if(countAllMatches("^$search$".toRegex())>0){
                        binding.resultTitle.text = getString(R.string.result_title_valid, search)
                    }else{
                        binding.resultTitle.text = getString(R.string.result_title_invalid, search)
                    }
                    binding.resultContent.text = ""
                }
                getString(R.string.search_mode_starting) -> {
                    //Words starting with
                    val wordCount = countAllMatches("^$search".toRegex())
                    val wordList = listAllMatches("^$search".toRegex())
                    binding.resultTitle.text = getString(R.string.result_title_starting, wordCount, search)
                    binding.resultContent.text = wordList.toString()
                }
            }
        }
    }
}