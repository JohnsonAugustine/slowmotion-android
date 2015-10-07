package me.hosiet.slowmotion;

import android.app.Activity;
import android.content.Context;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Spinner;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.Socket;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Communicator class
 *
 * Contains much of data-sending functions.
 * Note that all internet methods must run in background thread.
 *
 * Created by hosiet on 15-9-5.
 */
public class Communicator {
    /**
     * Connect and return the obtained socket.
     *
     * @param context Application Context
     * No parameters needed; all the data are obtained from Pref.
     */
    public static Socket smSocketConnect(Context context)
    {
        Socket socket = null;
        try {
            /* obtain settings from Preference Manager */
            InetAddress serverAddr = InetAddress.getByName(PreferenceManager
                    .getDefaultSharedPreferences(context.getApplicationContext())
                    .getString(context.getString(R.string.key_pref_remote_addr), "")
            );
            Integer serverPort = Integer.valueOf(PreferenceManager
                    .getDefaultSharedPreferences(context.getApplicationContext())
                    .getString(context.getString(R.string.key_pref_remote_port), "")
            );

            /* establish the socket */
            socket = new Socket(serverAddr, serverPort);
        } catch(Exception e) {
            e.printStackTrace();
        }
        Log.v("smSocketConnect()", "Socket connected");
        return socket;
    }

    public static boolean smSocketDisconnect(Context context, Socket socket) {
        if (socket == null) {
            Log.w("smSocketDisconnect()", "socket is null, ignoring.");
            return true;
        } else if (socket.isClosed()) {
            Log.w("smSocketDisconnect()", "socket is already closed, ignoring.");
            return true;
        }
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Log.i("smSocketDisconnect()", "socket disconnected normally.");
        return true;
    }

    public static Socket smSocketReconnect(Context context, Socket socket) {
        smSocketDisconnect(context, socket);
        return smSocketConnect(context);
    }

    public static void smSendCmd_ResetAll(Socket socket) {
        smSocketSendText(socket, "<command action=\"reset all\"/>");
    }

    public static void smSocketSendText(Socket socket, String xmlstr) {
        // Check input validity
        if (socket == null /* && ! valid xmlstr && socket hasn't been closed*/) {
            Log.e("Communicator", "Invalid socket or xmlstr");
            // TODO Toast?
            // TODO SOCKET DETECTION?
            // Valid str should be one line text
            return;
        }

        // begin sending
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())),
                    true);
            out.println(xmlstr);
            out.flush();
            if (out.checkError()) {
                // resend?
                Log.e("smSocketSendText()", "checked error.");
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * @param socket given socket
     */
    public static String smSocketGetText(Socket socket) {
        // Check input validity
        if (socket == null /* && ! valid xmlstr && socket hasn't been closed*/) {
            Log.e("Communicator", "Invalid socket");
            // TODO Toast?
            // TODO SOCKET DETECTION?
            // Valid str should be one line text
            return null;
        }
        Log.i("smSocketGetText()", "will now try to recv text.");

        // Receive all given data (by line)
        try {
            InputStream is = socket.getInputStream();
            BufferedReader lines = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = lines.readLine();
            if (line == null) {
                Log.e("smSocketGetText", "Got null line when recv!!");
            } else {
                Log.i("smSocketGetText(),", "Recv() success! line is " + line);
                return line;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Send requests for the music list.
     *
     * Will not parse it immediately. Later handled independently.
     */
    public static void smRequestMusicList(Activity activity) {
        /* Request for music list immediately. */
        Message msg = new Message();
        msg.what = DebugActivity.COMMAND_SEND;
        msg.obj = "<music action=\"get\" type=\"list\"/>";//"<command action=\"state music\"/>\n<command action=\"get\" type=\"list\"/>";
        DebugActivity.mHandler.sendMessage(msg);
        DebugActivity.status = "MUSIC";

        /* Also obtain music list now. */
        Message msg2 = new Message();
        msg2.what = DebugActivity.COMMAND_RECV;
        msg2.obj = activity;
        DebugActivity.mHandler.sendMessage(msg2);// Later check DebugActivity.received_string
    }

    /**
     * Send autoplay command to the server
     */
    public static void smCommandAutoplay(DebugActivity debugActivity, int id) {
        /* send a message. */
        /* msg: <autoplay num="X"/> */
        Message msg8 = new Message();
        msg8.what = DebugActivity.COMMAND_SEND;
        msg8.obj = "<autoplay num=\""+String.valueOf(id)+"\"/>";
        DebugActivity.mHandler.sendMessage(msg8);
    }

    /**
     * An example for XML processing.
     *
     * using DOM. Not very graceful.
     */
    public static void smXMLExample() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("roothere");
            doc.appendChild(rootElement);

            // child elements
            Element child = doc.createElement("sub");
            rootElement.appendChild(child);

            // set attributes
            child.setAttribute("attr1", "new");
            Attr attr = doc.createAttribute("ww");
            attr.setValue("ee");
            child.setAttributeNode(attr);

            // Save as string
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));

            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(doc.getDocumentElement());

            transformer.transform(source, result);
            String xmlStr = sw.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
