package me.hosiet.slowmotion;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Debug;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {link MusicFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MusicFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MusicFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private static boolean SHOULD_LOAD_VIEW = true;

    /* Music list variables */
    public ArrayList<String> al_fileName = new ArrayList<>();
    public ArrayList<Boolean> al_hasNote = new ArrayList<>();

    //private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MusicFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MusicFragment newInstance(String param1, String param2) {
        MusicFragment fragment = new MusicFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public MusicFragment() {
        // Required empty public constructor
    }

    /**
     * onCreate()
     *
     * Will check if the socket is null.
     *
     * If null, the fragment will refuse to be loaded and return to welcome status.
     * @param savedInstanceState bundle.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Check for null socket. */
        if (DebugActivity.socket == null || !DebugActivity.socket.isConnected()) {
            SHOULD_LOAD_VIEW = false;
            Message msg = new Message();
            msg.what = DebugActivity.REQUEST_RETURN_WELCOME;
            msg.obj = getActivity();
            ((DebugActivity) getActivity()).mainHandler.sendMessage(msg);
        } else {
            SHOULD_LOAD_VIEW = true;
        }

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    /**
     * onStart().
     *
     * Will check if the socket is null.
     *
     * If null, the fragment will refuse to be loaded and return to welcome status.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (DebugActivity.socket == null || !DebugActivity.socket.isConnected()) {
            Log.e("MusicFragment:onStart()", "invalid socket, not running onStart().");
            return;
        }

        /* Switch to MUSIC status and request for list. */
        Message msg = new Message();
        msg.what = DebugActivity.COMMAND_SEND;
        msg.obj = "<command action=\"state music\"/>\n<command action=\"get\" type=\"list\"/>";
        DebugActivity.mHandler.sendMessage(msg);
        DebugActivity.status = "MUSIC";

        /* Also obtain music list now */
        Message msg2 = new Message();
        msg2.what = DebugActivity.COMMAND_RECV;
        msg2.obj = getActivity();
        DebugActivity.mHandler.sendMessage(msg2);// Later check DebugActivity.received_string

        /* Initialize various event handlers */
        View.OnClickListener playMusicOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO FINISH IT
                Message msg = new Message();
                msg.what = DebugActivity.COMMAND_SEND;
                msg.obj = "<music action=\"play\" which=\"1\"/>";
                DebugActivity.mHandler.sendMessage(msg);
            }
        };
        View.OnClickListener stopMusicOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = new Message();
                msg.what = DebugActivity.COMMAND_SEND;
                msg.obj = "<music action=\"stop\"/>";
                DebugActivity.mHandler.sendMessage(msg);
            }
        };
        getActivity().findViewById(R.id.fragment_music_button_play)
                .setOnClickListener(playMusicOnClickListener);
        getActivity().findViewById(R.id.fragment_music_button_stop)
                .setOnClickListener(stopMusicOnClickListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DebugActivity.socket == null || !DebugActivity.socket.isConnected()) {
            Log.e("MusicFrag:onResume()", "invalid socket, not running onResume().");
            return;
        }
        /* load the song items here */
        /* we need to do both XML parse and Spinner adaption */
        if (DebugActivity.received_string == null) {
            Log.e("MusicFrag:onResume()", "empty recv_string for the list!");
            Toast.makeText(
                    getActivity().getApplicationContext(),
                    getActivity().getString(R.string.str_error_when_connecting)+":RECV_STR",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            String song_xmlstr = DebugActivity.received_string;
            Spinner spinner = (Spinner) getActivity().findViewById(R.id.fragment_music_spinner);

            // Now parse the XML string
            try {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document document = documentBuilder.parse(new ByteArrayInputStream(song_xmlstr.getBytes("UTF-8")));
                Element rootElement = document.getDocumentElement();
                NodeList items = rootElement.getElementsByTagName("music");
                for (int i=0; i<items.getLength(); i++) {
                    // get the metadata for each song
                    Element item = (Element) items.item(i);
                    al_fileName.add(item.getAttribute("filename"));
                    al_hasNote.add(Integer.valueOf(item.getAttribute("filename")) == 1);
                }
            } catch (javax.xml.parsers.ParserConfigurationException pe) {
                pe.printStackTrace();
            } catch (java.io.UnsupportedEncodingException ee) {
                Log.e("MusicFrag:onResume()", "Unsupported Encoding happened");
                ee.printStackTrace();
            } catch (org.xml.sax.SAXException se) {
                Log.e("MusicFrag:onResume()", "Unrecognized XML happened");
                se.printStackTrace();
            } catch (java.io.IOException ie) {
                Log.e("MusicFrag:onResume()", "IOException found");
                ie.printStackTrace();
            }

            // OK. XML Parsed, now begin to write to Spinner



        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (SHOULD_LOAD_VIEW) {
            return inflater.inflate(R.layout.fragment_music, container, false);
        } else {
            return inflater.inflate(R.layout.fragment_nothing, container, false);
        }
    }

    /** // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }**/

    /**
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }*/

    @Override
    public void onStop() {
        super.onStop();
        /* send Message to background Thread to do the reset */
        Log.i("MusicFragment", "Now running onStop() to RESET_ALL");
        Message msg = new Message();
        msg.what = DebugActivity.COMMAND_RESET_ALL;
        DebugActivity.mHandler.sendMessage(msg);
    }

    /**
     * onDetach().
     */
    @Override
    public void onDetach() {
        super.onDetach();
        /* send Message to background Thread to do the reset */
        Message msg = new Message();
        msg.what = DebugActivity.COMMAND_RESET_ALL;
        DebugActivity.mHandler.sendMessage(msg);
        //mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    /**
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }**/

}
