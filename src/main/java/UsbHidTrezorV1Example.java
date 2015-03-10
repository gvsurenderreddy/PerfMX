import org.hid4java.*;
import org.hid4java.event.HidServicesEvent;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * <p>Demonstrate the USB HID interface using a production Bitcoin Trezor</p>
 *
 * @since 0.0.1
 *  
 */
public class UsbHidTrezorV1Example implements HidServicesListener {

    static final int PACKET_LENGTH = 64;
    private HidServices hidServices;

    public static void main(String[] args) throws HidException {

        UsbHidTrezorV1Example example = new UsbHidTrezorV1Example();
        example.executeExample();

    }

    public void executeExample() throws HidException {

        System.out.println("Loading hidapi...");

        // Get HID services
        hidServices = HidManager.getHidServices();
        hidServices.addHidServicesListener(this);

        System.out.println("Enumerating attached devices...");

        // Provide a list of attached devices
     /*   for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
            System.out.println(hidDevice);
        }*/

        // Open the Trezor device by Vendor ID and Product ID with wildcard serial number
        HidDevice trezor = hidServices.getHidDevice(0x046d, -15061, null);
        if (trezor != null) {
            // Device is already attached so send message
            sendInitialise(trezor);
        } else {
            System.out.println("Waiting for Trezor attach...");
        }
        // Stop the main thread to demonstrate attach and detach events
       // sleepUninterruptibly(5, TimeUnit.SECONDS);

        //System.out.println("stan urządzeni open="+trezor.isOpen());
        if (trezor != null && trezor.isOpen()) {
            trezor.close();
            System.out.println("zamykam urządzenie");
        }

        System.exit(0);
    }

    @Override
    public void hidDeviceAttached(HidServicesEvent event) {

        System.out.println("Device attached: " + event);

        if (event.getHidDevice().getVendorId() == 0x534c &&
                event.getHidDevice().getProductId() == 0x01) {

            // Open the Trezor device by Vendor ID and Product ID with wildcard serial number
            HidDevice trezor = hidServices.getHidDevice(0x534c, 0x01, null);
            if (trezor != null) {
                sendInitialise(trezor);
            }

        }

    }

    @Override
    public void hidDeviceDetached(HidServicesEvent event) {

        System.err.println("Device detached: " + event);

    }

    @Override
    public void hidFailure(HidServicesEvent event) {

        System.err.println("HID failure: " + event);

    }

    private void sendInitialise(HidDevice trezor) {

        // Send the Initialise message
        byte[] message = new byte[7/*8*/];int i=0;
       // message[i++] = 0x0;
        message[i++] = 0x10; //short request
        message[i++] = (byte)0x07; //device id
        message[i++] = (byte) 0x00;
        message[i++] = 0x10;//SwID
        message[i++]=0x00;
        message[i++]=0x00;
        message[i++]= (byte)0x0d;//ping data

        //https://julien.danjou.info/blog/2012/logitech-k750-linux-support

        for (int j=0; j<9; j++) {
            message[1/*2*/]=(byte)j;
            int val = trezor.write(message, i, (byte) 0x10);

            if (val >=0 ) {
                System.out.println(j+ "> [" + val + "]");
            } else {
                System.err.println(j+ trezor.getLastErrorMessage());
            }
        }

    }

    /**
     * Invokes {@code unit.}{@link java.util.concurrent.TimeUnit#sleep(long) sleep(sleepFor)}
     * uninterruptibly.
     */
    public static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
        boolean interrupted = false;
        try {
            long remainingNanos = unit.toNanos(sleepFor);
            long end = System.nanoTime() + remainingNanos;
            while (true) {
                try {
                    // TimeUnit.sleep() treats negative timeouts just like zero.
                    NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
