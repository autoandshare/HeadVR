package autoandshare.headvr.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.fragment.app.Fragment;
import autoandshare.headvr.R;

public class AboutFragment extends Fragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.about_ui, null);
        return root;
    }

    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        WebView help = getView().findViewById(R.id.help);
        help.loadData(readAsset("help.html"), "text/html", "utf-8");
    }

    private String readAsset(String name) {
        StringBuilder buf = new StringBuilder();

        try (InputStream stream = getActivity().getAssets().open(name)) {
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                buf.append(line).append("\n");
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf.toString();
    }


}
