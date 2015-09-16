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
import android.widget.EditText;


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
    private static boolean SHOULD_LOAD_VIEW = true;

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

        if (DebugActivity.socket == null || !DebugActivity.socket.isConnected()) {
            Log.e("NoteFragment::onStart()", "invalid socket, not running onStart().");
            return;
        }

        /* Switch to USERPLAY status. */
        Message msg = new Message();
        msg.what = DebugActivity.COMMAND_SEND;
        msg.obj = "<command action=\"state userplay\"/>";
        DebugActivity.mHandler.sendMessage(msg);
        DebugActivity.status = "USERPLAY";

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
                }
            }
        };
        for (int i = 1; i <= 8; i++) {
            getActivity().findViewById(getResources().getIdentifier("note_button_0"+Integer.toString(i), "id", getActivity().getPackageName()))
                    .setOnClickListener(notePlayOnClickListener);
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
