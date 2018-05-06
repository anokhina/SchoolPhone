/*
 * Copyright 2018 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.org.sevn.schoolphone.andr;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetUtil {

    public static String getMACAddress(final NetworkInterface networkInterface) throws SocketException {
        byte[] mac = networkInterface.getHardwareAddress();
        if (mac==null) return "";
        StringBuilder buf = new StringBuilder();
        for (int idx=0; idx<mac.length; idx++)
            buf.append(String.format("%02X:", mac[idx]));
        if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
        return buf.toString();
    }

    public static String getMACAddress(String interfaceName) {
        try {
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaceEnumeration.hasMoreElements()) {
                final NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();

                if (interfaceName != null) {
                    if (!networkInterface.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                return getMACAddress(networkInterface);
            }
        } catch (Exception ex) { }
        return null;
    }

    public static String getIPAddress(final boolean useIPv4, final InetAddress inetAddress) {
        String hostAddress = inetAddress.getHostAddress();
        boolean isIPv4 = hostAddress.indexOf(':')<0;

        if (useIPv4) {
            if (hostAddress.indexOf(':') < 0) {
                return hostAddress;
            }
        } else {
            if (!isIPv4) {
                int delim = hostAddress.indexOf('%');
                if (delim < 0) {
                    return hostAddress.toUpperCase();
                } else {
                    return hostAddress.substring(0, delim).toUpperCase();
                }
            }
        }
        return null;
    }

    public static String getIPAddress(final boolean useIPv4) {
        try {
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaceEnumeration.hasMoreElements()) {
                final NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
                Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
                while(inetAddressEnumeration.hasMoreElements()) {
                    final InetAddress inetAddress = inetAddressEnumeration.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return getIPAddress(useIPv4, inetAddress);
                    }
                }
            }
        } catch (Exception ex) { }
        return null;
    }
}
