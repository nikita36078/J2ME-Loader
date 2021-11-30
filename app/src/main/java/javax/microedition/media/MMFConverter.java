/*
 * Copyright 2021 ohayoyogi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.media;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MMFConverter {
    static final byte FORMAT_TYPE_HANDY_PHONE = 0x00;
    static final byte FORMAT_TYPE_MOBILE_STANDARD_COMPRESS = 0x01;
    static final byte FORMAT_TYPE_MOBILE_STANDARD_NO_COMPRESS = 0x02;

    private abstract class TrackEvent {
        int timestamp;

        TrackEvent(int timestamp) {
            this.timestamp = timestamp;
        }

        public int getTimestamp() {
            return timestamp;
        }

        abstract void output(DataOutputStream dos, int currentTime) throws IOException;
    }

    private class NOPMessage extends TrackEvent {
        public NOPMessage(int timestamp) {
            super(timestamp);
        }

        @Override
        void output(DataOutputStream dos, int currentTime) throws IOException {
            System.out.println(String.format("timestamp: %d, NOP", timestamp));
        }
    }

    private class EOSMessage extends TrackEvent {
        public EOSMessage(int timestamp) {
            super(timestamp);
        }

        @Override
        void output(DataOutputStream dos, int currentTime) throws IOException {
            writeVariableLengthValue(timestamp - currentTime, dos);
            dos.write(new byte[] { (byte) 0xFF, 0x20, 0x00 });
            System.out.println(String.format("timestamp: %d, EOS", timestamp));
        }
    }

    private class NoteMessage extends TrackEvent {
        int channel;
        int noteNumber;
        int keyVelocity;
        int gateTime;
        boolean noteOn;

        public NoteMessage(int timestamp, boolean noteOn, int channel, int noteNumber, int keyVelocity, int gateTime) {
            super(timestamp);
            this.channel = channel;
            this.noteOn = noteOn;
            this.noteNumber = noteNumber;
            this.keyVelocity = keyVelocity;
            this.gateTime = gateTime;
        }

        void output(DataOutputStream dos, int currentTime) throws IOException {
            // if (channel == 1) return;
            writeVariableLengthValue(timestamp - currentTime, dos);
            if (noteOn) {
                dos.writeByte(0x9F & (0xF0 | channel));
            } else {
                dos.writeByte(0x8F & (0xF0 | channel));
            }
            dos.writeByte(noteNumber);
            dos.writeByte(keyVelocity);
        }
    }

    private class ControlChange extends TrackEvent {
        int channel;
        int controlNumber;
        int controlValue;

        public ControlChange(int timestamp, int channel, int controlNumber, int controlValue) {
            super(timestamp);
            this.channel = channel;
            this.controlNumber = controlNumber;
            this.controlValue = controlValue;
        }

        @Override
        void output(DataOutputStream dos, int currentTime) throws IOException {
            // not implemented
        }
    }

    private class ProgramChange extends TrackEvent {
        int channel;
        int programNumber;

        public ProgramChange(int timestamp, int channel, int programNumber) {
            super(timestamp);
            this.channel = channel;
            this.programNumber = programNumber;
        }

        @Override
        void output(DataOutputStream dos, int currentTime) throws IOException {
            writeVariableLengthValue(timestamp - currentTime, dos);
            dos.writeByte(0xCF & ((byte) 0xF0 | channel));
            dos.writeByte(programNumber);
        }
    }

    private class PitchBend extends TrackEvent {
        int channel;
        int pitchBendChangeLSB;
        int pitchBendChangeMSB;

        public PitchBend(int channel, int pitchBendChangeLSB, int pitchBendChangeMSB) {
            super(0);
            this.channel = channel;
            this.pitchBendChangeLSB = pitchBendChangeLSB;
            this.pitchBendChangeMSB = pitchBendChangeMSB;
        }

        @Override
        void output(DataOutputStream dos, int currentTime) {
            // not implemented
        }
    }

    
    int readVariableLengthValue(DataInputStream dis) throws IOException {
        int result = 0;
        boolean readNext = true;
        while (readNext) {
            byte next = dis.readByte();
            result = (result << 7) + (next & (byte) 0x7F);
            readNext = (next & (byte) 0x80) == (byte) 0x80;
        }
        return result;
    }

    void writeVariableLengthValue(int val, DataOutputStream dos) throws IOException {
        ArrayList<Byte> bytes = new ArrayList<>();
        int tmp = val;
        bytes.add((byte) (tmp & 0x7F)); // 0b01111111
        while ((tmp = tmp >>> 7) > 0) {
            bytes.add((byte) ((tmp & 0x7F) | (byte) 0x80));
        }
        Collections.reverse(bytes);
        for (Byte byte1 : bytes) {
            dos.writeByte(byte1);
        }
    }

    String toHex(byte v) {
        String result = "0x";
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        int v_ = v & 0xFF;
        result += hexArray[v_ >>> 4];
        result += hexArray[v_ & 0x0F];
        return result;
    }

    ArrayList<TrackEvent> parseSequenceDataChunk(byte[] data) throws Exception {
        ArrayList<TrackEvent> result = new ArrayList<>();
        Map<Integer, Integer> mapVel = new HashMap<>();

        try (ByteArrayInputStream is = new ByteArrayInputStream(data); DataInputStream dis = new DataInputStream(is)) {
            // parse Duration;
            byte[] firstByte = new byte[1];
            int globalTime = 0;
            while (dis.read(firstByte, 0, 1) == 1) {
                int duration = firstByte[0] & 0x7F;
                if ((firstByte[0] & 0b10000000) == 0b10000000) {
                    // MSB = 1
                    duration = (duration << 7) + (dis.readByte() & 0x7F);
                }

                // parse Event
                byte eventHeader = dis.readByte();
                globalTime += duration;
                int upper4bit = (eventHeader & 0xFF) >>> 4;
                switch (upper4bit) {
                    case 0x8: {
                        // Note Message with no velocity
                        int channel = eventHeader & (byte) 0x0F;
                        int noteNumber = dis.readByte() & (byte) 0x7F;
                        // notenumber 60 equals center C
                        int gateTime = readVariableLengthValue(dis);
                        Integer prevVel = mapVel.get(channel);
                        if (prevVel == null) {
                            prevVel = 64;
                            mapVel.put(channel, prevVel);
                        }
                        result.add(new NoteMessage(globalTime, true, channel, noteNumber, prevVel, gateTime));
                        result.add(new NoteMessage(globalTime + gateTime, false, channel, noteNumber, 0, gateTime));
                        break;
                    }
                    case 0x9: {
                        // Note Message with velocity
                        int channel = eventHeader & 0x0F;
                        int noteNumber = dis.readByte() & 0x7F;
                        int keyVelocity = dis.readByte() & 0x7F;
                        int gateTime = readVariableLengthValue(dis);
                        mapVel.put(channel, keyVelocity);
                        result.add(new NoteMessage(globalTime, true, channel, noteNumber, keyVelocity, gateTime));
                        result.add(new NoteMessage(globalTime + gateTime, false, channel, noteNumber, 0, gateTime));
                        break;
                    }
                    case 0xA: {
                        // Reserved
                        throw new Exception("Not Implemented");
                        // break;
                    }
                    case 0xB: {
                        // Control Change
                        int channel = eventHeader & 0x0F;
                        int controlNumber = dis.readByte() & 0x7F;
                        int controlValue = dis.readByte() & 0x7F;
                        result.add(new ControlChange(globalTime, channel, controlNumber, controlValue));
                        break;
                    }
                    case 0xC: {
                        // Program Change
                        int channel = eventHeader & 0x0F;
                        int programNumber = dis.readByte() & 0x7F;
                        result.add(new ProgramChange(globalTime, channel, programNumber));
                        break;
                    }
                    case 0xD: { // Reserved
                        throw new Exception("Not Implemented");
                        // break;
                    }
                    case 0xE: {
                        // Pitch Bend
                        int channel = eventHeader & 0x0F;
                        int pitchBendChangeLSB = dis.readByte() & 0x7F;
                        int pitchBendChangeMSB = dis.readByte() & 0x7F;
                        result.add(new PitchBend(globalTime, pitchBendChangeLSB, pitchBendChangeMSB));
                        break;
                    }
                    case 0xF: {
                        if (eventHeader == (byte) 0xF0) {
                            // System Exclusive
                            int x = readVariableLengthValue(dis);
                            byte[] dataToEnd = new byte[x];
                            if (dis.read(dataToEnd, 0, x) != x || dataToEnd[x - 1] != (byte) 0xF7) {
                                throw new Exception("Malformed");
                            }
                        } else if (eventHeader == (byte) 0xFF) {
                            // EOS or NOP
                            byte next = dis.readByte();
                            if ((byte) 0x00 == next) {
                                result.add(new NOPMessage(globalTime));
                            } else if ((byte) 0x2F == next) {
                                if (dis.readByte() == (byte) 0x00) {
                                    result.add(new EOSMessage(globalTime));
                                } else {
                                    throw new Exception("Not Implemented");
                                }
                            } else {
                                throw new Exception("Not Implemented");
                            }
                        } else {
                            // Reserved
                        }
                        break;
                    }
                    default:
                        throw new Exception("Not Implemented: " + toHex((byte) upper4bit));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    int timeBaseD;
    int timeBaseG;

    int convertTimebase(byte timebase) {
        switch (timebase) {
            case 0x01:
                return 2;
            case 0x02:
                return 4;
            case 0x03:
                return 5;
            case 0x10:
                return 10;
            case 0x11:
                return 20;
            case 0x12:
                return 40;
            case 0x13:
                return 50;
            case 0x00:
            default:
                return 1;
        }
    }

    void parseMTR(byte data[], DataOutputStream dos) throws Exception {
        try (ByteArrayInputStream is = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(is)) {
            byte formatType = dis.readByte();
            byte sequenceType = dis.readByte();
            this.timeBaseD = convertTimebase(dis.readByte());
            this.timeBaseG = convertTimebase(dis.readByte());

            byte[] channelStatus;
            if (formatType == FORMAT_TYPE_MOBILE_STANDARD_COMPRESS
                    || formatType == FORMAT_TYPE_MOBILE_STANDARD_NO_COMPRESS) {
                // read 16byte channel status
                channelStatus = new byte[16];
                dis.read(channelStatus, 0, 16);
            }

            byte[] chunkId = new byte[4];
            while (dis.read(chunkId, 0, 4) == 4) {
                // Mspl (Seek & Phrase Info Chunk)
                // Mtsu (Setup Data Chunk)
                // Mtsq (Sequence Data Chunk)
                if (Arrays.equals(chunkId, "Mtsq".getBytes())) {
                    int chunkSize = dis.readInt();
                    byte[] chunkDat = new byte[chunkSize];
                    if (dis.read(chunkDat, 0, chunkSize) != chunkSize)
                        throw new Exception("chunkSize is not match: " + chunkSize);
                    ArrayList<TrackEvent> trk = parseSequenceDataChunk(chunkDat);
                    dos.write("MThd".getBytes());
                    dos.writeInt(6);
                    dos.write(new byte[]{0x00, 0x01}); // format
                    dos.write(new byte[]{0x00, 0x01}); // track num
                    dos.write(new byte[]{0x01, (byte) 0xE0}); // time (defined by tempo and time)
                    dos.write("MTrk".getBytes());

                    try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                         DataOutputStream ddos = new DataOutputStream(os)) {
                        // set tempo
                        int timeBase = 480;
                        int tempo = timeBase * 1000 * timeBaseD;
                        ddos.write(new byte[]{0x00, (byte) 0xFF, 0x51, 0x03});
                        ddos.writeByte((tempo >>> 16) & (byte) 0xFF);
                        ddos.writeByte((tempo >>> 8) & (byte) 0xFF);
                        ddos.writeByte((tempo >>> 0) & (byte) 0xFF);

                        Collections.sort(trk, new Comparator<TrackEvent>() {
                            @Override
                            public int compare(TrackEvent o1, TrackEvent o2) {
                                return o1.getTimestamp() - o2.getTimestamp();
                            }
                        });

                        int globalTime = 0;
                        for (TrackEvent trackEvent : trk) {
                            trackEvent.output(ddos, globalTime);
                            globalTime = trackEvent.getTimestamp();
                        }
                        dos.writeInt(ddos.size());
                        dos.write(os.toByteArray());
                    }
                }
                // Mtsp (Stream PCM Data Chunk)
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] convertToMDI(byte[] data) throws Exception {
        byte[] magic = new byte[4];
        try (ByteArrayInputStream is = new ByteArrayInputStream(data); DataInputStream dis = new DataInputStream(is)) {
            dis.read(magic, 0, 4);
            if (!Arrays.equals(magic, "MMMD".getBytes())) {
                throw new Exception("Signature is not match MMMD: " + magic);
            }
            dis.read(magic, 0, 4);
            if (!Arrays.equals(magic, "CNTI".getBytes())) {
                throw new Exception("Signature is not match CNTI: " + magic);
            }
            int blockSize = dis.readInt();
            byte[] cntiBlock = new byte[blockSize];
            dis.read(cntiBlock, 0, blockSize);

            dis.read(magic, 0, 4);
            if (!Arrays.equals(magic, "OPDA".getBytes())) {
                throw new Exception("Signature is not match OPDA: " + magic);
            }
            blockSize = dis.readInt();
            byte[] blockData = new byte[blockSize];
            dis.read(blockData, 0, blockSize);

            dis.read(magic, 0, 4);
            if (!Arrays.equals(Arrays.copyOf(magic, 3), "MTR".getBytes())) {
                throw new Exception("Signature is not match MTR: " + new String(magic));
            }
            blockSize = dis.readInt();
            blockData = new byte[blockSize];
            if (dis.read(blockData, 0, blockSize) != blockSize) {
                throw new Exception("blockSize does not match");
            }
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 DataOutputStream dos = new DataOutputStream(bos)) {
                parseMTR(blockData, dos);
                return bos.toByteArray();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new Exception("track is not found");
    }
}

