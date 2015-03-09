package com.ds;

import java.util.List;

import javax.usb.*;
import javax.usb.UsbDeviceDescriptor;

public class Main {

    private static void dumpDevice(final UsbDevice device)
    {
        // Dump information about the device itself
        System.out.println(device);
        final UsbPort port = device.getParentUsbPort();
        if (port != null)
        {
            System.out.println("Connected to port: " + port.getPortNumber());
            System.out.println("Parent: " + port.getUsbHub());
        }

        // Dump device descriptor
        System.out.println(device.getUsbDeviceDescriptor());

        // Process all configurations
        for (UsbConfiguration configuration: (List<UsbConfiguration>) device
                .getUsbConfigurations())
        {
            // Dump configuration descriptor
            System.out.println(configuration.getUsbConfigurationDescriptor());

            // Process all interfaces
            for (UsbInterface iface: (List<UsbInterface>) configuration
                    .getUsbInterfaces())
            {
                // Dump the interface descriptor
                System.out.println(iface.getUsbInterfaceDescriptor());

                // Process all endpoints
                for (UsbEndpoint endpoint: (List<UsbEndpoint>) iface
                        .getUsbEndpoints())
                {
                    // Dump the endpoint descriptor
                    System.out.println(endpoint.getUsbEndpointDescriptor());
                }
            }
        }

        System.out.println();

        // Dump child devices if device is a hub
        if (device.isUsbHub())
        {
            final UsbHub hub = (UsbHub) device;
            for (UsbDevice child: (List<UsbDevice>) hub.getAttachedUsbDevices())
            {
                dumpDevice(child);
            }
        }
    }
    public static UsbDevice findDevice(UsbHub hub, short vendorId, short productId)
    {
        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
        {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
            if (device.isUsbHub())
            {
                device = findDevice((UsbHub) device, vendorId, productId);
                if (device != null) return device;
            }
        }
        return null;
    }

    public static short deviceVid=(short)0x046d;
    public static short devicePid=(short)0xc52b;
    private static UsbDevice deviceInstance;

    public static void main(final String[] args) throws UsbException
    {
        deviceInstance = findDevice(UsbHostManager.getUsbServices().getRootUsbHub(), deviceVid, devicePid);
        dumpDevice(deviceInstance);

        UsbConfiguration configuration = deviceInstance.getActiveUsbConfiguration();
        UsbInterface iface = configuration.getUsbInterface((byte)0);
        iface.claim(new UsbInterfacePolicy()
        {
            @Override
            public boolean forceClaim(UsbInterface usbInterface)
            {
                return true;
            }
        }); /* <==
                this fails on windows -
                "Please note that interface policies are just a hint for the underlying USB implementation.
                In case of usb4java the policy will be ignored on Windows because
                ** libusb doesn't support detaching drivers on Windows.** "
                ~ http://usb4java.org/quickstart/javax-usb.html
            */ //unfortunatelly this closes chance of uf using usb4java on this project (hence java is going to be replaced with C# .NET)

        try
        {
            UsbEndpoint endpoint = iface.getUsbEndpoint((byte)0x83);
            UsbPipe pipe = endpoint.getUsbPipe();
            pipe.open();
            try
            {
                int sent = pipe.syncSubmit(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
                System.out.println(sent + " bytes sent");
            }
            finally
            {
                pipe.close();
            }
        }
        finally
        {
            iface.release();
        }

    }
}
