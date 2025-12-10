package edu.ptithcm.network.core;

import edu.ptithcm.model.NetworkPacket;
import edu.ptithcm.model.Peer;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class PeerConnection {
    private volatile boolean running;
    private Peer peer;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private SecretKey sessionKey;
    private long lastHeartbeat;


    public void send(NetworkPacket packet){
        //parse to json
        //encrypt
        //send
    }

    public void startListening(){
        //get data
        //convert to json
        //decrypt
        //convert to NetworkPacket
        // handle packet
    }

    public void close(){
        try{
            running = false;
            socket.close();
        }catch (Exception e){}
    }
}
