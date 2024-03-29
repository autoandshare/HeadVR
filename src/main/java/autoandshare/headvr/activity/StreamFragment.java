package autoandshare.headvr.activity;

import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.Arrays;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.browse.HeadVRChannel;

public class StreamFragment extends Fragment {

    private MMKV historyStreams;
    private EditText urlText;
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stream, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MMKV.initialize(getContext());
        historyStreams = MMKV.mmkvWithID("StreamUrls");

        String savedUrls = historyStreams.getString("history", "").trim();
        if (savedUrls.length() != 0) {
            listItems = new ArrayList<String>(Arrays.asList(
                    savedUrls.split("\\n")));
        }

        initUI(getView());
    }

    private String pastedUrl = null;

    @Override
    public void onResume() {
        super.onResume();
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            String text = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
            if (URLUtil.isValidUrl(text) && !text.equals(pastedUrl)) {
                setUrlText(text);
                pastedUrl = text;
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        historyStreams.putString("history", TextUtils.join("\n", listItems));
    }

    private void initUI(View view) {
        urlText = view.findViewById(R.id.streamUrl);

        Button channelButton = view.findViewById(R.id.buttonHeadvrChannel);
        channelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HeadVRChannel.setChannelURL(false);
                VlcHelper.openHeadVRChannel(getContext());
            }
        });
        channelButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                HeadVRChannel.setChannelURL(true);
                VlcHelper.openHeadVRChannel(getContext());
                return true;
            }
        });

        view.findViewById(R.id.buttonPlayStream).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlText.getText().toString().trim();
                addOrReplace(url);
                VlcHelper.openUri(getContext(), Uri.parse(url));
            }
        });

        adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1, listItems);

        mListView = view.findViewById(R.id.streamUrlList);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setUrlText(listItems.get(position));
            }
        });

        view.findViewById(R.id.clearUrlList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clear();
            }
        });

    }

    private void setUrlText(String text) {
        urlText.setText(text);
        urlText.selectAll();
    }

    private void addOrReplace(String url) {
        listItems.remove(url);
        listItems.add(0, url);
        if (listItems.size() >= 100) {
            listItems.remove(listItems.size() - 1);
        }
        adapter.notifyDataSetChanged();
    }
}