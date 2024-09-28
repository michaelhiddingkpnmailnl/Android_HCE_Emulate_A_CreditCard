package de.androidcrypto.android_hce_emulate_a_creditcard;

import static de.androidcrypto.android_hce_emulate_a_creditcard.InternalFilesHelper.readBinaryDataFromInternalStorage;
import static de.androidcrypto.android_hce_emulate_a_creditcard.InternalFilesHelper.writeTextToInternalStorage;
import static de.androidcrypto.android_hce_emulate_a_creditcard.Utils.arrayBeginsWith;
import static de.androidcrypto.android_hce_emulate_a_creditcard.Utils.bytesToHexNpe;
import static de.androidcrypto.android_hce_emulate_a_creditcard.Utils.concatenateByteArrays;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.GET_PROCESSING_OPTONS_COMMAND;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.GET_PROCESSING_OPTONS_COMMAND_START;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.GET_PROCESSING_OPTONS_RESPONSE;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.READ_FILE_10_03_COMMAND;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.READ_FILE_10_03_RESPONSE;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.READ_FILE_10_04_COMMAND;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.READ_FILE_10_04_RESPONSE;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.READ_FILE_10_05_COMMAND;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.READ_FILE_10_05_RESPONSE;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.READ_FILE_10_06_COMMAND;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.READ_FILE_10_06_RESPONSE;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.SELECT_AID_COMMAND;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.SELECT_AID_RESPONSE;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.SELECT_OK_SW;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.SELECT_PPSE_COMMAND;
import static de.androidcrypto.android_hce_emulate_a_creditcard.VisaSampleCard.SELECT_PPSE_RESPONSE;

import android.content.Context;
import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HceCcEmulationService2 extends HostApduService {

    private static final String CARD_EMULATION_FILENAME = "cardemulation.txt"; // any changes need to done in MainActivity and HceCcEmulationService
    public Context context;
    private boolean readCarEmulation = true; // true on start up and when a PPSE is requested
    private int selectCard = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplication(); // get the application context here
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle bundle) {
        sendMessageToActivity("# processCommandApdu received", bytesToHexNpe(commandApdu));
        //sendUsingStaticallyRegisteredBroadcastReceiver("# processCommandApdu received " + bytesToHexNpe(bytes));
        System.out.println("processCommandApdu: " + bytesToHexNpe(commandApdu));
        System.out.println("processCommandApdu: " + new String(commandApdu, StandardCharsets.UTF_8));

        // don't ask for the cardEmulation when in a reading sequence, reset on SELECT_PPSE only
        // int selectCard = -1;
        if (readCarEmulation) {
            selectCard = readCardEmulationFromInternalStorage();
        }
        //int selectCard = -1; // -1 = just SW_OK, 0 = Visa, 1 = Mastercard AAB, 2 = Mastercard Lloyds

        // this command is independent from a card type
        // step 01
        if (Arrays.equals(commandApdu, SELECT_PPSE_COMMAND)) {
            // step 01 selecting the PPSE
            sendMessageToActivity("step 01 Select PPSE Command ", bytesToHexNpe(commandApdu));
            // sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(SELECT_PPSE_RESPONSE));
            selectCard = readCardEmulationFromInternalStorage();
            System.out.println("HCE2 selectCard: " + selectCard);
            readCarEmulation = false;
            // define the different returns here
            if (selectCard == -1) {
                return SELECT_OK_SW;
            } else if (selectCard == 0) {
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            } else if (selectCard == 1) {
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(MastercardSampleCardAab.SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardAab.SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            } else if (selectCard == 2) {
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(MastercardSampleCardLloyds.SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardLloyds.SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            } else if (selectCard == 3) {
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(VisaAnonymizedSampleCard.SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(VisaAnonymizedSampleCard.SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            } else if (selectCard == 4) {
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(MastercardAnonymizedSampleCard.SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(MastercardAnonymizedSampleCard.SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            } else if (selectCard == 5) {
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(GirocardVobaRf.SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(GirocardVobaRf.SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            }
        }

        if (selectCard == 0) {
            // Visacard
            sendMessageToActivity("# Visa Card #", "");
            /*
            // step 01
            if (Arrays.equals(commandApdu, SELECT_PPSE_COMMAND)) {
                // step 01 selecting the PPSE
                sendMessageToActivity("step 01 Select PPSE Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            }
            */

            // step 02
            if (Arrays.equals(commandApdu, SELECT_AID_COMMAND)) {
                // step 02 selecting the AID
                sendMessageToActivity("step 02 Select AID Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 02 Select AID Response", bytesToHexNpe(SELECT_AID_RESPONSE));
                return concatenateByteArrays(SELECT_AID_RESPONSE, SELECT_OK_SW);
            }

            // step 03
            // step 03 Get Processing Options
            // todo do not compare the full commandApdu as it is changed to my sample data
            // if (Arrays.equals(commandApdu, VisaAnonymizedSampleCard.SGET_PROCESSING_OPTONS_COMMAND)) {
            if (arrayBeginsWith(commandApdu, GET_PROCESSING_OPTONS_COMMAND_START)) {
                sendMessageToActivity("step 03 Get Processing Options (GPO) Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 03 Get Processing Options (GPO) Response", bytesToHexNpe(GET_PROCESSING_OPTONS_RESPONSE));
                return concatenateByteArrays(GET_PROCESSING_OPTONS_RESPONSE, SELECT_OK_SW);
            }

            // step 04
            if (Arrays.equals(commandApdu, READ_FILE_10_03_COMMAND)) {
                // step 04 Read File 10/03 Command
                sendMessageToActivity("step 04 Read File 10/03 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 04 Read File 10/03 Response", bytesToHexNpe(READ_FILE_10_03_RESPONSE));
                return concatenateByteArrays(READ_FILE_10_03_RESPONSE, SELECT_OK_SW);
            }

            // step 05
            if (Arrays.equals(commandApdu, READ_FILE_10_04_COMMAND)) {
                // step 05 Read File 10/04 Command
                sendMessageToActivity("step 05 Read File 10/04 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 05 Read File 10/05 Response", bytesToHexNpe(READ_FILE_10_04_RESPONSE));
                return concatenateByteArrays(READ_FILE_10_04_RESPONSE, SELECT_OK_SW);
            }

            // step 06
            if (Arrays.equals(commandApdu, READ_FILE_10_05_COMMAND)) {
                // step 06 Read File 10/05 Command
                sendMessageToActivity("step 06 Read File 10/05 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 06 Read File 10/05 Response", bytesToHexNpe(READ_FILE_10_05_RESPONSE));
                return concatenateByteArrays(READ_FILE_10_05_RESPONSE, SELECT_OK_SW);
            }

            // step 07
            if (Arrays.equals(commandApdu, READ_FILE_10_06_COMMAND)) {
                // step 07 Read File 10/06 Command
                sendMessageToActivity("step 08 Read File 10/06 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 08 Read File 10/06 Response", bytesToHexNpe(READ_FILE_10_06_RESPONSE));
                return concatenateByteArrays(READ_FILE_10_06_RESPONSE, SELECT_OK_SW);
            }
        } else if (selectCard == 1) {
            // Mastercard AAB
            sendMessageToActivity("# Mastercard AAB #", "");
            /*
            // step 01
            if (Arrays.equals(commandApdu, MastercardSampleCardAab.SELECT_PPSE_COMMAND)) {
                // step 01 selecting the PPSE
                sendMessageToActivity("step 01 Select PPSE Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(MastercardSampleCardAab.SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardAab.SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            }
             */

            // step 02
            if (Arrays.equals(commandApdu, MastercardSampleCardAab.SELECT_AID_COMMAND)) {
                // step 02 selecting the AID
                sendMessageToActivity("step 02 Select AID Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 02 Select AID Response", bytesToHexNpe(MastercardSampleCardAab.SELECT_AID_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardAab.SELECT_AID_RESPONSE, SELECT_OK_SW);
            }

            // step 03
            if (Arrays.equals(commandApdu, MastercardSampleCardAab.GET_PROCESSING_OPTONS_COMMAND)) {
                // step 03 Get Processing Options
                sendMessageToActivity("step 03 Get Processing Options (GPO) Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 03 Get Processing Options (GPO) Response", bytesToHexNpe(MastercardSampleCardAab.GET_PROCESSING_OPTONS_RESPONSE_ORG));
                return concatenateByteArrays(MastercardSampleCardAab.GET_PROCESSING_OPTONS_RESPONSE_ORG, SELECT_OK_SW);
            }

            // step 04
            if (Arrays.equals(commandApdu, MastercardSampleCardAab.READ_FILE_08_01_COMMAND)) {
                // step 04 Read File 08/01 Command
                sendMessageToActivity("step 04 Read File 08/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 04 Read File 08/01 Response", bytesToHexNpe(MastercardSampleCardAab.READ_FILE_08_01_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardAab.READ_FILE_08_01_RESPONSE, SELECT_OK_SW);
            }

            // step 05
            if (Arrays.equals(commandApdu, MastercardSampleCardAab.READ_FILE_10_01_COMMAND)) {
                // step 05 Read File 10/01 Command
                sendMessageToActivity("step 05 Read File 10/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 05 Read File 10/01 Response", bytesToHexNpe(MastercardSampleCardAab.READ_FILE_10_01_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardAab.READ_FILE_10_01_RESPONSE, SELECT_OK_SW);
            }

            // step 06
            if (Arrays.equals(commandApdu, MastercardSampleCardAab.READ_FILE_20_01_COMMAND)) {
                // step 06 Read File 20/01 Command
                sendMessageToActivity("step 06 Read File 20/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 06 Read File 20/01 Response", bytesToHexNpe(MastercardSampleCardAab.READ_FILE_20_01_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardAab.READ_FILE_20_01_RESPONSE, SELECT_OK_SW);
            }

            // step 07
            if (Arrays.equals(commandApdu, MastercardSampleCardAab.READ_FILE_20_02_COMMAND)) {
                // step 07 Read File 20/02 Command
                sendMessageToActivity("step 08 Read File 20/02 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 08 Read File 20/02 Response", bytesToHexNpe(MastercardSampleCardAab.READ_FILE_20_02_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardAab.READ_FILE_20_02_RESPONSE, SELECT_OK_SW);
            }


        } else if (selectCard == 2) {
            // Mastercard Lloyds
            sendMessageToActivity("# Mastercard Lloyds #", "");
            /*
            // step 01
            if (Arrays.equals(commandApdu, MastercardSampleCardLloyds.SELECT_PPSE_COMMAND)) {
                // step 01 selecting the PPSE
                sendMessageToActivity("step 01 Select PPSE Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(MastercardSampleCardLloyds.SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardLloyds.SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            }
             */

            // step 02
            if (Arrays.equals(commandApdu, MastercardSampleCardLloyds.SELECT_AID_COMMAND)) {
                // step 02 selecting the AID
                sendMessageToActivity("step 02 Select AID Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 02 Select AID Response", bytesToHexNpe(MastercardSampleCardLloyds.SELECT_AID_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardLloyds.SELECT_AID_RESPONSE, SELECT_OK_SW);
            }

            // step 03
            if (Arrays.equals(commandApdu, MastercardSampleCardLloyds.GET_PROCESSING_OPTONS_COMMAND)) {
                // step 03 Get Processing Options
                sendMessageToActivity("step 03 Get Processing Options (GPO) Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 03 Get Processing Options (GPO) Response", bytesToHexNpe(MastercardSampleCardLloyds.GET_PROCESSING_OPTONS_RESPONSE_ORG));
                return concatenateByteArrays(MastercardSampleCardLloyds.GET_PROCESSING_OPTONS_RESPONSE_ORG, SELECT_OK_SW);
            }

            // step 04
            if (Arrays.equals(commandApdu, MastercardSampleCardLloyds.READ_FILE_08_01_COMMAND)) {
                // step 04 Read File 08/01 Command
                sendMessageToActivity("step 04 Read File 08/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 04 Read File 08/01 Response", bytesToHexNpe(MastercardSampleCardLloyds.READ_FILE_08_01_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardLloyds.READ_FILE_08_01_RESPONSE, SELECT_OK_SW);
            }

            // step 05
            if (Arrays.equals(commandApdu, MastercardSampleCardLloyds.READ_FILE_10_01_COMMAND)) {
                // step 05 Read File 10/01 Command
                sendMessageToActivity("step 05 Read File 10/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 05 Read File 10/01 Response", bytesToHexNpe(MastercardSampleCardLloyds.READ_FILE_10_01_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardLloyds.READ_FILE_10_01_RESPONSE, SELECT_OK_SW);
            }

            // step 06
            if (Arrays.equals(commandApdu, MastercardSampleCardLloyds.READ_FILE_10_02_COMMAND)) {
                // step 05 Read File 10/02 Command
                sendMessageToActivity("step 06 Read File 10/02 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 06 Read File 10/02 Response", bytesToHexNpe(MastercardSampleCardLloyds.READ_FILE_10_02_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardLloyds.READ_FILE_10_02_RESPONSE, SELECT_OK_SW);
            }

            // step 07
            if (Arrays.equals(commandApdu, MastercardSampleCardLloyds.READ_FILE_18_01_COMMAND)) {
                // step 07 Read File 18/01 Command
                sendMessageToActivity("step 07 Read File 18/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 07 Read File 18/01 Response", bytesToHexNpe(MastercardSampleCardLloyds.READ_FILE_18_01_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardLloyds.READ_FILE_18_01_RESPONSE, SELECT_OK_SW);
            }

            // step 08
            if (Arrays.equals(commandApdu, MastercardSampleCardLloyds.READ_FILE_18_02_COMMAND)) {
                // step 08 Read File 18/02 Command
                sendMessageToActivity("step 08 Read File 18/02 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 08 Read File 18/02 Response", bytesToHexNpe(MastercardSampleCardLloyds.READ_FILE_18_02_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardLloyds.READ_FILE_18_02_RESPONSE, SELECT_OK_SW);
            }

            // step 09
            if (Arrays.equals(commandApdu, MastercardSampleCardLloyds.READ_FILE_20_01_COMMAND)) {
                // step 09 Read File 20/01 Command
                sendMessageToActivity("step 09 Read File 20/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 09 Read File 20/01 Response", bytesToHexNpe(MastercardSampleCardLloyds.READ_FILE_20_01_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardLloyds.READ_FILE_20_01_RESPONSE, SELECT_OK_SW);
            }

            // step 10
            if (Arrays.equals(commandApdu, MastercardSampleCardLloyds.READ_FILE_20_02_COMMAND)) {
                // step 10 Read File 20/02 Command
                sendMessageToActivity("step 10 Read File 20/02 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 10 Read File 20/02 Response", bytesToHexNpe(MastercardSampleCardLloyds.READ_FILE_20_02_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardLloyds.READ_FILE_20_02_RESPONSE, SELECT_OK_SW);
            }
        } else if (selectCard == 3) {
            // Visacard
            sendMessageToActivity("# Visa Card Emulated #", "");
            System.out.println("# Visa Card Emulated #");
            /*
            // step 01
            if (Arrays.equals(commandApdu, SELECT_PPSE_COMMAND)) {
                // step 01 selecting the PPSE
                sendMessageToActivity("step 01 Select PPSE Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            }
            */

            // step 02
            if (Arrays.equals(commandApdu, VisaAnonymizedSampleCard.SELECT_AID_COMMAND)) {
                // step 02 selecting the AID
                sendMessageToActivity("step 02 Select AID Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 02 Select AID Response", bytesToHexNpe(VisaAnonymizedSampleCard.SELECT_AID_RESPONSE));
                return concatenateByteArrays(VisaAnonymizedSampleCard.SELECT_AID_RESPONSE, SELECT_OK_SW);
            }

            // step 03
            // todo do not compare the full commandApdu as it is changed to my sample data
            // if (Arrays.equals(commandApdu, VisaAnonymizedSampleCard.SGET_PROCESSING_OPTONS_COMMAND)) {
            if (arrayBeginsWith(commandApdu, VisaAnonymizedSampleCard.GET_PROCESSING_OPTONS_COMMAND_START)) {
                // step 03 Get Processing Options
                sendMessageToActivity("step 03 Get Processing Options (GPO) Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 03 Get Processing Options (GPO) Response", bytesToHexNpe(VisaAnonymizedSampleCard.GET_PROCESSING_OPTONS_RESPONSE));
                return concatenateByteArrays(VisaAnonymizedSampleCard.GET_PROCESSING_OPTONS_RESPONSE, SELECT_OK_SW);
            }

            // step 04
            if (Arrays.equals(commandApdu, VisaAnonymizedSampleCard.READ_FILE_10_02_COMMAND)) {
                // step 04 Read File 10/02 Command
                sendMessageToActivity("step 04 Read File 10/02 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 04 Read File 10/02 Response", bytesToHexNpe(VisaAnonymizedSampleCard.READ_FILE_10_02_RESPONSE));
                return concatenateByteArrays(VisaAnonymizedSampleCard.READ_FILE_10_02_RESPONSE, SELECT_OK_SW);
            }

            // step 05
            if (Arrays.equals(commandApdu, VisaAnonymizedSampleCard.READ_FILE_10_03_COMMAND)) {
                // step 05 Read File 10/03 Command
                sendMessageToActivity("step 05 Read File 10/03 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 05 Read File 10/03 Response", bytesToHexNpe(VisaAnonymizedSampleCard.READ_FILE_10_03_RESPONSE));
                return concatenateByteArrays(VisaAnonymizedSampleCard.READ_FILE_10_03_RESPONSE, SELECT_OK_SW);
            }

            // step 06
            if (Arrays.equals(commandApdu, VisaAnonymizedSampleCard.READ_FILE_10_04_COMMAND)) {
                // step 06 Read File 10/04 Command
                sendMessageToActivity("step 06 Read File 10/04 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 06 Read File 10/05 Response", bytesToHexNpe(VisaAnonymizedSampleCard.READ_FILE_10_04_RESPONSE));
                return concatenateByteArrays(VisaAnonymizedSampleCard.READ_FILE_10_04_RESPONSE, SELECT_OK_SW);
            }
        } else if (selectCard == 4) {
            // Mastercard Modified
            sendMessageToActivity("# Mastercard Emulated #", "");
            /*
            // step 01
            if (Arrays.equals(commandApdu, MastercardSampleCardAab.SELECT_PPSE_COMMAND)) {
                // step 01 selecting the PPSE
                sendMessageToActivity("step 01 Select PPSE Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(MastercardSampleCardAab.SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardAab.SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            }
             */

            // step 02
            if (Arrays.equals(commandApdu, MastercardAnonymizedSampleCard.SELECT_AID_COMMAND)) {
                // step 02 selecting the AID
                sendMessageToActivity("step 02 Select AID Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 02 Select AID Response", bytesToHexNpe(MastercardAnonymizedSampleCard.SELECT_AID_RESPONSE));
                return concatenateByteArrays(MastercardAnonymizedSampleCard.SELECT_AID_RESPONSE, SELECT_OK_SW);
            }

            // step 03
            if (Arrays.equals(commandApdu, MastercardAnonymizedSampleCard.GET_PROCESSING_OPTONS_COMMAND)) {
                // step 03 Get Processing Options
                sendMessageToActivity("step 03 Get Processing Options (GPO) Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 03 Get Processing Options (GPO) Response", bytesToHexNpe(MastercardAnonymizedSampleCard.GET_PROCESSING_OPTONS_RESPONSE_ORG));
                return concatenateByteArrays(MastercardAnonymizedSampleCard.GET_PROCESSING_OPTONS_RESPONSE_ORG, SELECT_OK_SW);
            }

            // step 04
            if (Arrays.equals(commandApdu, MastercardAnonymizedSampleCard.READ_FILE_08_01_COMMAND)) {
                // step 04 Read File 08/01 Command
                sendMessageToActivity("step 04 Read File 08/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 04 Read File 08/01 Response", bytesToHexNpe(MastercardAnonymizedSampleCard.READ_FILE_08_01_RESPONSE));
                return concatenateByteArrays(MastercardAnonymizedSampleCard.READ_FILE_08_01_RESPONSE, SELECT_OK_SW);
            }

            // step 05
            if (Arrays.equals(commandApdu, MastercardAnonymizedSampleCard.READ_FILE_10_01_COMMAND)) {
                // step 05 Read File 10/01 Command
                sendMessageToActivity("step 05 Read File 10/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 05 Read File 10/01 Response", bytesToHexNpe(MastercardAnonymizedSampleCard.READ_FILE_10_01_RESPONSE));
                return concatenateByteArrays(MastercardAnonymizedSampleCard.READ_FILE_10_01_RESPONSE, SELECT_OK_SW);
            }

            // step 06
            if (Arrays.equals(commandApdu, MastercardAnonymizedSampleCard.READ_FILE_20_01_COMMAND)) {
                // step 06 Read File 20/01 Command
                sendMessageToActivity("step 06 Read File 20/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 06 Read File 20/01 Response", bytesToHexNpe(MastercardAnonymizedSampleCard.READ_FILE_20_01_RESPONSE));
                return concatenateByteArrays(MastercardAnonymizedSampleCard.READ_FILE_20_01_RESPONSE, SELECT_OK_SW);
            }

            // step 07
            if (Arrays.equals(commandApdu, MastercardAnonymizedSampleCard.READ_FILE_20_02_COMMAND)) {
                // step 07 Read File 20/02 Command
                sendMessageToActivity("step 08 Read File 20/02 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 08 Read File 20/02 Response", bytesToHexNpe(MastercardAnonymizedSampleCard.READ_FILE_20_02_RESPONSE));
                return concatenateByteArrays(MastercardAnonymizedSampleCard.READ_FILE_20_02_RESPONSE, SELECT_OK_SW);
            }
        } else if (selectCard == 5) {
            // Girocard VoBa Raesfeld RF
            sendMessageToActivity("# VoBa Raesf RF Emulated #", "");
            /*
            // step 01
            if (Arrays.equals(commandApdu, MastercardSampleCardAab.SELECT_PPSE_COMMAND)) {
                // step 01 selecting the PPSE
                sendMessageToActivity("step 01 Select PPSE Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 01 Select PPSE Response", bytesToHexNpe(MastercardSampleCardAab.SELECT_PPSE_RESPONSE));
                return concatenateByteArrays(MastercardSampleCardAab.SELECT_PPSE_RESPONSE, SELECT_OK_SW);
            }
             */

            // step 02
            if (Arrays.equals(commandApdu, GirocardVobaRf.SELECT_AID_COMMAND)) {
                // step 02 selecting the AID
                sendMessageToActivity("step 02 Select AID Command ", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 02 Select AID Response", bytesToHexNpe(GirocardVobaRf.SELECT_AID_RESPONSE));
                return concatenateByteArrays(GirocardVobaRf.SELECT_AID_RESPONSE, SELECT_OK_SW);
            }

            // step 03
            // todo do not compare the full commandApdu as it is changed to my sample data
            // if (Arrays.equals(commandApdu, GirocardVobaRf.SGET_PROCESSING_OPTONS_COMMAND)) {
            if (arrayBeginsWith(commandApdu, GirocardVobaRf.GET_PROCESSING_OPTONS_COMMAND_START)) {
                // step 03 Get Processing Options
                sendMessageToActivity("step 03 Get Processing Options (GPO) Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 03 Get Processing Options (GPO) Response", bytesToHexNpe(GirocardVobaRf.GET_PROCESSING_OPTONS_RESPONSE));
                return concatenateByteArrays(GirocardVobaRf.GET_PROCESSING_OPTONS_RESPONSE, SELECT_OK_SW);
            }

            // in total 2 files in SFI 18 (01, 02), 1 file in SFI 20 (01), 1 file in SFI 40 (04), 3 files in SFI 08 (05, 07, 03) (7 files total)
            // step 04
            if (Arrays.equals(commandApdu, GirocardVobaRf.READ_FILE_18_01_COMMAND)) {
                // step 04 Read File 08/01 Command
                sendMessageToActivity("step 04 Read File 18/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 04 Read File 18/01 Response", bytesToHexNpe(GirocardVobaRf.READ_FILE_18_01_RESPONSE));
                return concatenateByteArrays(GirocardVobaRf.READ_FILE_18_01_RESPONSE, SELECT_OK_SW);
            }

            // step 05
            if (Arrays.equals(commandApdu, GirocardVobaRf.READ_FILE_18_02_COMMAND)) {
                // step 04 Read File 08/01 Command
                sendMessageToActivity("step 05 Read File 18/02 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 05 Read File 18/02 Response", bytesToHexNpe(GirocardVobaRf.READ_FILE_18_02_RESPONSE));
                return concatenateByteArrays(GirocardVobaRf.READ_FILE_18_02_RESPONSE, SELECT_OK_SW);
            }

            // step 06
            if (Arrays.equals(commandApdu, GirocardVobaRf.READ_FILE_20_01_COMMAND)) {
                // step 04 Read File 08/01 Command
                sendMessageToActivity("step 06 Read File 20/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 06 Read File 20/01 Response", bytesToHexNpe(GirocardVobaRf.READ_FILE_20_01_RESPONSE));
                return concatenateByteArrays(GirocardVobaRf.READ_FILE_20_01_COMMAND, SELECT_OK_SW);
            }

            // step 07
            if (Arrays.equals(commandApdu, GirocardVobaRf.READ_FILE_40_04_COMMAND)) {
                // step 04 Read File 08/01 Command
                sendMessageToActivity("step 07 Read File 40/04 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 07 Read File 40/04 Response", bytesToHexNpe(GirocardVobaRf.READ_FILE_40_04_RESPONSE));
                return concatenateByteArrays(GirocardVobaRf.READ_FILE_40_04_RESPONSE, SELECT_OK_SW);
            }

            // step 08
            if (Arrays.equals(commandApdu, GirocardVobaRf.READ_FILE_08_05_COMMAND)) {
                // step 04 Read File 08/01 Command
                sendMessageToActivity("step 04 Read File 08/05 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 04 Read File 08/05 Response", bytesToHexNpe(GirocardVobaRf.READ_FILE_08_05_RESPONSE));
                return concatenateByteArrays(GirocardVobaRf.READ_FILE_08_05_RESPONSE, SELECT_OK_SW);
            }

            // step 09
            if (Arrays.equals(commandApdu, GirocardVobaRf.READ_FILE_08_07_COMMAND)) {
                // step 04 Read File 08/01 Command
                sendMessageToActivity("step 04 Read File 08/07 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 04 Read File 08/07 Response", bytesToHexNpe(GirocardVobaRf.READ_FILE_08_07_RESPONSE));
                return concatenateByteArrays(GirocardVobaRf.READ_FILE_08_07_RESPONSE, SELECT_OK_SW);
            }

            // step 10
            if (Arrays.equals(commandApdu, GirocardVobaRf.READ_FILE_08_03_COMMAND)) {
                // step 04 Read File 08/01 Command
                sendMessageToActivity("step 10 Read File 08/03 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 10 Read File 08/03 Response", bytesToHexNpe(GirocardVobaRf.READ_FILE_08_03_RESPONSE));
                return concatenateByteArrays(GirocardVobaRf.READ_FILE_08_03_RESPONSE, SELECT_OK_SW);
            }


            /*
            // step 05
            if (Arrays.equals(commandApdu, MastercardAnonymizedSampleCard.READ_FILE_10_01_COMMAND)) {
                // step 05 Read File 10/01 Command
                sendMessageToActivity("step 05 Read File 10/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 05 Read File 10/01 Response", bytesToHexNpe(MastercardAnonymizedSampleCard.READ_FILE_10_01_RESPONSE));
                return concatenateByteArrays(MastercardAnonymizedSampleCard.READ_FILE_10_01_RESPONSE, SELECT_OK_SW);
            }

            // step 06
            if (Arrays.equals(commandApdu, MastercardAnonymizedSampleCard.READ_FILE_20_01_COMMAND)) {
                // step 06 Read File 20/01 Command
                sendMessageToActivity("step 06 Read File 20/01 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 06 Read File 20/01 Response", bytesToHexNpe(MastercardAnonymizedSampleCard.READ_FILE_20_01_RESPONSE));
                return concatenateByteArrays(MastercardAnonymizedSampleCard.READ_FILE_20_01_RESPONSE, SELECT_OK_SW);
            }

            // step 07
            if (Arrays.equals(commandApdu, MastercardAnonymizedSampleCard.READ_FILE_20_02_COMMAND)) {
                // step 07 Read File 20/02 Command
                sendMessageToActivity("step 08 Read File 20/02 Command", bytesToHexNpe(commandApdu));
                sendMessageToActivity("step 08 Read File 20/02 Response", bytesToHexNpe(MastercardAnonymizedSampleCard.READ_FILE_20_02_RESPONSE));
                return concatenateByteArrays(MastercardAnonymizedSampleCard.READ_FILE_20_02_RESPONSE, SELECT_OK_SW);
            }

             */
        }

        return SELECT_OK_SW;
    }

    private int readCardEmulationFromInternalStorage() {
        byte[] content = readBinaryDataFromInternalStorage(context, CARD_EMULATION_FILENAME, null);
        if (content == null) {
            // no file created so far
            return -1;
        }
        String cardEmulation = new String(content, StandardCharsets.UTF_8);
        if (cardEmulation.equals("-1")) {
            return -1;
        } else if (cardEmulation.equals("0")) {
            return 0;
        } else if (cardEmulation.equals("1")) {
            return 1;
        } else if (cardEmulation.equals("2")) {
            return 2;
        } else if (cardEmulation.equals("3")) {
            return 3;
        } else if (cardEmulation.equals("4")) {
            return 4;
        } else if (cardEmulation.equals("5")) {
            return 5;
        } else {
            return -1;
        }
    }

    @Override
    public void onDeactivated(int reason) {
        // is called when the connection between this device and a NFC card reader ends
        sendMessageToActivity("onDeactivated with reason", String.valueOf(reason));
        if (reason == DEACTIVATION_LINK_LOSS) sendMessageToActivity("deactivated because of DEACTIVATION_LINK_LOSS", "");
        if (reason == DEACTIVATION_DESELECTED) sendMessageToActivity("deactivated because of DEACTIVATION_DESELECTED", "");
    }

/*
    public void sendUsingStaticallyRegisteredBroadcastReceiver(String message) {
        Intent intent = new Intent();
        intent.setAction("HceCcEmulatorService");
        //intent.putExtra("Message", message);
        Context context = this;
        context.sendBroadcast(intent);
        System.out.println("HCE broadcast sent");
    }
*/

    /**
     * A utility method to send data with an Broadcast Intent from this service to the MainActivity class
     *
     * @param msg
     * @param data
     */
    private void sendMessageToActivity(String msg, String data) {
        Intent intent = new Intent("HceCcEmulatorService");
        // You can also include some extra data.
        intent.putExtra("Message", msg);
        intent.putExtra("Data", data);
        /*
        Bundle b = new Bundle();
        b.putParcelable("Location", l);
        intent.putExtra("Location", b);

         */
        Context context = this;
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
