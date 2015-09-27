package me.hosiet.slowmotion;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Debug;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {link NoteFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NoteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NoteFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private static boolean SHOULD_LOAD_VIEW = true; // Determined by weather valid socket

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
     * @return A new instance of fragment NoteFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NoteFragment newInstance(String param1, String param2) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public NoteFragment() {
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

        /* Check for null|invalid socket. */
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

    @Override
    public void onStart() {
        super.onStart();
        Log.i("onStart()", "this is NoteFragment::onStart()");

        if (DebugActivity.socket == null || !DebugActivity.socket.isConnected()) {
            Log.e("NoteFragment::onStart()", "invalid socket, not running onStart().");
            return;
        }

        /* Request for music list immediately. */
        Communicator.smRequestMusicList(getActivity());

        /* set up Button Listener. */
        View.OnClickListener notePlayOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Directly send out message */
                if (DebugActivity.socket == null || ! DebugActivity.socket.isConnected()) {
                    // Bad socket state. Return to Welcome Now.
                    Log.e("NoteFragment::onStart()", "failed in checking socket, resetting");
                    Message msg = new Message();
                    msg.obj = getActivity();
                    msg.what = DebugActivity.REQUEST_RETURN_WELCOME;
                    DebugActivity.socket = null;
                    DebugActivity.status = null;
                    ((DebugActivity) getActivity()).mainHandler.sendMessage(msg);
                } else {
                    Message msg = new Message();
                    msg.what = DebugActivity.COMMAND_SEND;
                    msg.obj = "<play note=\""+v.getTag().toString()+"\"/>";
                    DebugActivity.mHandler.sendMessage(msg);
                    // recorder
                    if (DebugActivity.al_recNoteTimeBegin != 0) {
                        // record current playing into DebugActivity.al_recNote{Name, Time}
                        String tagstr = v.getTag().toString();
                        Long timehere = System.currentTimeMillis();
                        Log.i("Button onClick()", "Now recording note play"+tagstr+" time:"+timehere.toString());
                        DebugActivity.al_recNoteName.add(tagstr);
                        DebugActivity.al_recNoteTime.add(timehere);
                    }
                }
            }
        };
        for (int i = 1; i <= 12; i++) {
            getActivity().findViewById(getResources().getIdentifier("note_button_"+Integer.toString(i), "id", getActivity().getPackageName()))
                    .setOnClickListener(notePlayOnClickListener);
        }


        /* set up music button listeners */
        View.OnClickListener musicPlayOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Directly send out play message */
                if (DebugActivity.socket == null || ! DebugActivity.socket.isConnected()) {
                    // Bad socket state. Return to Welcome Now.
                    Log.e("NoteFragment::onStart()", "failed in checking socket, resetting");
                    Message msg = new Message();
                    msg.obj = getActivity();
                    msg.what = DebugActivity.REQUEST_RETURN_WELCOME;
                    DebugActivity.socket = null;
                    DebugActivity.status = null;
                    ((DebugActivity) getActivity()).mainHandler.sendMessage(msg);
                } else {
                    Message msg = new Message();
                    Message msg2 = new Message();
                    msg.what = DebugActivity.COMMAND_SEND;
                    msg2.what = DebugActivity.COMMAND_SEND;
                    Integer pos = al_fileName.indexOf(
                            ((Spinner) getActivity()
                                    .findViewById(R.id.fragment_music_spinner))
                                    .getSelectedItem()
                                    .toString()
                    ) + 1;
                    String yes_music_loop = "no";
                    if (PreferenceManager.getDefaultSharedPreferences(
                            getActivity().getApplicationContext())
                            .getBoolean(getActivity().getString(R.string.key_pref_music_loop), false)) {
                        // Send extra command first
                        yes_music_loop = "yes";
                    }
                    msg2.obj = "<music loop=\""+yes_music_loop+"\"/>";
                    msg.obj = "<music action=\"play\" which=\""+String.valueOf(pos)+"\"/>";
                    DebugActivity.mHandler.sendMessage(msg);
                    DebugActivity.mHandler.sendMessage(msg2);
                }
            }
        };
        (getActivity().findViewById(R.id.fragment_music_button_play)).setOnClickListener(musicPlayOnClickListener);
        View.OnClickListener musicStopOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Directly send out play message */
                if (DebugActivity.socket == null || ! DebugActivity.socket.isConnected()) {
                    // Bad socket state. Return to Welcome Now.
                    Log.e("NoteFragment::onStart()", "failed in checking socket, resetting");
                    Message msg = new Message();
                    msg.obj = getActivity();
                    msg.what = DebugActivity.REQUEST_RETURN_WELCOME;
                    DebugActivity.socket = null;
                    DebugActivity.status = null;
                    ((DebugActivity) getActivity()).mainHandler.sendMessage(msg);
                } else {
                    Message msg = new Message();
                    msg.what = DebugActivity.COMMAND_SEND;
                    msg.obj = "<music action=\"stop\"/>";
                    DebugActivity.mHandler.sendMessage(msg);
                }
            }
        };
        (getActivity().findViewById(R.id.fragment_music_button_stop)).setOnClickListener(musicStopOnClickListener);


        /* !!!!!!!!!! For music volume up and down !!!!!!!!!! */
        View.OnClickListener musicVolumeUpOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Directly send out message */
                if (DebugActivity.socket == null || ! DebugActivity.socket.isConnected()) {
                    // Bad socket state. Return to Welcome Now.
                    Log.e("NoteFragment::onStart()", "failed in checking socket, resetting");
                    Message msg = new Message();
                    msg.obj = getActivity();
                    msg.what = DebugActivity.REQUEST_RETURN_WELCOME;
                    DebugActivity.socket = null;
                    DebugActivity.status = null;
                    ((DebugActivity) getActivity()).mainHandler.sendMessage(msg);
                } else {
                    Message msg = new Message();
                    msg.what = DebugActivity.COMMAND_SEND;
                    msg.obj = "<music sound=\"up\"/>";
                    DebugActivity.mHandler.sendMessage(msg);
                }
            }
        };
        (getActivity().findViewById(R.id.fragment_music_button_volume_up)).setOnClickListener(musicVolumeUpOnClickListener);

        View.OnClickListener musicVolumeDownOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Directly send out message */
                if (DebugActivity.socket == null || ! DebugActivity.socket.isConnected()) {
                    // Bad socket state. Return to Welcome Now.
                    Log.e("NoteFragment::onStart()", "failed in checking socket, resetting");
                    Message msg = new Message();
                    msg.obj = getActivity();
                    msg.what = DebugActivity.REQUEST_RETURN_WELCOME;
                    DebugActivity.socket = null;
                    DebugActivity.status = null;
                    ((DebugActivity) getActivity()).mainHandler.sendMessage(msg);
                } else {
                    Message msg = new Message();
                    msg.what = DebugActivity.COMMAND_SEND;
                    msg.obj = "<music sound=\"down\"/>";
                    DebugActivity.mHandler.sendMessage(msg);
                }
            }
        };
        (getActivity().findViewById(R.id.fragment_music_button_volume_down)).setOnClickListener(musicVolumeDownOnClickListener);

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
                    getActivity().getString(R.string.str_error_when_connecting) + ":RECV_STR_ERR",
                    Toast.LENGTH_SHORT
            ).show();
        } else {

            /* parse the XML of received_string and transform into ArrarList */
            String song_xmlstr = DebugActivity.received_string;
            // TODO NOTE ONLY FOR DEBUG HERE!!! FIXME
            //song_xmlstr = "<musiclist><music id=\"1\" filename=\"123.mp3\" havenote=\"1\"/><music id=\"2\" filename=\"234.mp3\" havenote=\"0\"/></musiclist>";
            Spinner spinner = (Spinner) getActivity().findViewById(R.id.fragment_music_spinner);

            // Now parse the XML string
            try {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Log.e("parsing", "now song_xmlstr is "+song_xmlstr);
                Document document = documentBuilder.parse(new ByteArrayInputStream(song_xmlstr.getBytes("UTF-8")));
                Element rootElement = document.getDocumentElement();
                NodeList items = rootElement.getElementsByTagName("music");
                for (int i = 0; i < items.getLength(); i++) {
                    // get the metadata for each song
                    Element item = (Element) items.item(i);
                    al_fileName.add(item.getAttribute("filename"));
                    al_hasNote.add(Integer.valueOf(item.getAttribute("havenote")) == 1);
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

            /* OK. XML Parsed, now begin to write to Spinner */
            // First, convert arraylist to string
            String[] adaptStringList = new String[al_fileName.size()];
            adaptStringList = al_fileName.toArray(adaptStringList);
            // Then link adapter with Spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity().getApplicationContext(), R.layout.spinner_default, adaptStringList);
            adapter.setDropDownViewResource(R.layout.spinner_default);
            spinner.setAdapter(adapter);
            spinner.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (SHOULD_LOAD_VIEW) {
            return inflater.inflate(R.layout.fragment_note, container, false);
        } else {
            return inflater.inflate(R.layout.fragment_nothing, container, false);
        }
    }

    /**
    // TODO: Rename method, update argument and hook method into UI event
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
    }**/

    @Override
    public void onStop() {
        super.onStop();
        /* send Message to background Thread to do the reset */
        Log.i("NoteFragment", "Now running onStop() to RESET_ALL");
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
