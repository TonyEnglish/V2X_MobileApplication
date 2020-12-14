package com.wzdctool.android.ui.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.wzdctool.android.R


/**
 *  Help page
 *
 *
 */

class HelpFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.help_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Open online documentation (README)
        view.findViewById<Button>(R.id.more_button).setOnClickListener() {
            val url = "https://github.com/TonyEnglish/V2X_MobileApplication"
//            val url = "https://github.com/TonyEnglish/Work_Zone_Data_Collection_Toolset/blob/master/Documentation/WZDC%20Tool%20Documentation%20Updates.pdf/"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        // Start send email intent
        view.findViewById<Button>(R.id.access_button).setOnClickListener() {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto","tony@neaeraconsulting.com", null))
            intent.putExtra(Intent.EXTRA_SUBJECT, "WZDC Tool Access");
            intent.putExtra(Intent.EXTRA_TEXT, "Requesting access to WZDC Tool azure storage information");
            startActivity(Intent.createChooser(intent, ""));
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }
}