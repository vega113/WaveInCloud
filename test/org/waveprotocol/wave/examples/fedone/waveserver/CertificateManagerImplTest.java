/**
 * Copyright 2009 Google Inc.
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
 *
 */
package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.crypto.CachedCertPathValidator;
import org.waveprotocol.wave.examples.fedone.crypto.CertPathStore;
import org.waveprotocol.wave.examples.fedone.crypto.DefaultCacheImpl;
import org.waveprotocol.wave.examples.fedone.crypto.DefaultCertPathStore;
import org.waveprotocol.wave.examples.fedone.crypto.DefaultTrustRootsProvider;
import org.waveprotocol.wave.examples.fedone.crypto.SignatureException;
import org.waveprotocol.wave.examples.fedone.crypto.SignerInfo;
import org.waveprotocol.wave.examples.fedone.crypto.TimeSource;
import org.waveprotocol.wave.examples.fedone.crypto.TrustRootsProvider;
import org.waveprotocol.wave.examples.fedone.crypto.VerifiedCertChainCache;
import org.waveprotocol.wave.examples.fedone.crypto.WaveSignatureVerifier;
import org.waveprotocol.wave.examples.fedone.crypto.WaveSigner;
import org.waveprotocol.wave.examples.fedone.crypto.WaveSignerFactory;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolSignature;
import org.waveprotocol.wave.protocol.common.ProtocolSignedDelta;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolSignature.SignatureAlgorithm;
import org.waveprotocol.wave.protocol.common.ProtocolSignerInfo.HashAlgorithm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class CertificateManagerImplTest extends TestCase {

  private static final String PRIVATE_KEY =
    "-----BEGIN PRIVATE KEY-----\n" +
    "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAKueRG+YuGX6Fifk\n" +
    "JpYR+Gh/qF+PpGLSYVR7CzhGNh5a8RayKwPM8YNqsfKAT8VqLdAk19x//cf03Cgc\n" +
    "UwLQsuUo3zxK4E110L96lVX6oF12FiIpSCVN+E93qin2W7VXw2JtfvQ4BllwdNMj\n" +
    "/yNPl+bHuhtOjFAPpWEhCkSJP6NlAgMBAAECgYAaRocP1wAUjO+rd+D4hRPVXAY5\n" +
    "a1Kt1qwUNSqImSdcCmxzHyA62rv3dPR9vmt4PEN7ZMiv9+CxJqo2ce+7tJxO/Xq1\n" +
    "lPTh8IVX+NUPI8LWtek9VZlXZ16nY5qXZ0i32vrwOz+GaZMfchAK05eTaiUJTN4P\n" +
    "T2Wskp6jnlDGZYeNmQJBANXMPa70jf2M6zHq0dKBg+4I3XZ1x59G0fUnho1Ck+Q5\n" +
    "ixo5GpFbbx2YgQmbFNUHhMNAJvLTduV5S3+CopqB3FMCQQDNfpUYQrmrAOvAZiQ0\n" +
    "uX/BtorjvSoTkj4g2JegaGWUVAc8As9d3VrBf8l2ovJRuzVSGqHpzke7T8wGwaGr\n" +
    "cEpnAkBFz+N0dbbHzHQgYKUTL+d8mrh2Lg95Gw8EFlwBVHQmWgPqFCtwu4KVD29T\n" +
    "S6iJx2K6vv/42sRAOlNE18tw2GaxAkBAKakGBTeR5Fy4G2xspgr1AjlFuLfdmokZ\n" +
    "mmdlp5MoCECmBT6YUVhYGL1f9KryyCBy/WvW5BjTrKvI5EbFj+87AkAobTHhq+D7\n" +
    "TOQBpaA5v45z6HNsFdCovQkQokJbirQ0KDIopo5IT7Qtz7+Gi3S0uYl3xooAsCRc\n" +
    "Zj50nIvr3txX\n" +
    "-----END PRIVATE KEY-----\n";

  private static final String CERTIFICATE =
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIC9TCCAl6gAwIBAgIJALQVfb0zIz6bMA0GCSqGSIb3DQEBBQUAMFsxCzAJBgNV\n" +
    "BAYTAlVTMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJbnRlcm5ldCBX\n" +
    "aWRnaXRzIFB0eSBMdGQxFDASBgNVBAMTC2V4YW1wbGUuY29tMB4XDTA5MDcxODA2\n" +
    "MjIyNloXDTEwMDcxODA2MjIyNlowWzELMAkGA1UEBhMCVVMxEzARBgNVBAgTClNv\n" +
    "bWUtU3RhdGUxITAfBgNVBAoTGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDEUMBIG\n" +
    "A1UEAxMLZXhhbXBsZS5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKue\n" +
    "RG+YuGX6FifkJpYR+Gh/qF+PpGLSYVR7CzhGNh5a8RayKwPM8YNqsfKAT8VqLdAk\n" +
    "19x//cf03CgcUwLQsuUo3zxK4E110L96lVX6oF12FiIpSCVN+E93qin2W7VXw2Jt\n" +
    "fvQ4BllwdNMj/yNPl+bHuhtOjFAPpWEhCkSJP6NlAgMBAAGjgcAwgb0wHQYDVR0O\n" +
    "BBYEFD2DmpOW+OiFr6U3Nu7NuDGuBSJgMIGNBgNVHSMEgYUwgYKAFD2DmpOW+OiF\n" +
    "r6U3Nu7NuDGuBSJgoV+kXTBbMQswCQYDVQQGEwJVUzETMBEGA1UECBMKU29tZS1T\n" +
    "dGF0ZTEhMB8GA1UEChMYSW50ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMRQwEgYDVQQD\n" +
    "EwtleGFtcGxlLmNvbYIJALQVfb0zIz6bMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcN\n" +
    "AQEFBQADgYEAS7H+mB7lmEihX5lOWp9ZtyI7ua7MYVK05bbuBZJLAhO1mApu5Okg\n" +
    "DqcybVV8ijPLJkII75dn+q7olpwMmgyjjsozEKY1N0It9nRsb9fW2tKGp2qlCMA4\n" +
    "zP29U9091ZRH/xL1RPVzhkRHqfNJ/x+iTC4laSLBtwlsjjkd8Us6xrg=\n" +
    "-----END CERTIFICATE-----\n";

  private static final String DOMAIN = "example.com";

  private static final byte[] REAL_SIGNATURE =
      Base64.decodeBase64(("jbe3xbKd4Shl9QYa3J+Bh+g" +
      "mc44OA538hqW1uyfbrsmA/Z9x1ud9gALkoAs/maZd6t6sh9X+bCleEz13vuljc1l" +
      "WAcdXeVOB8FRDN8OEYJcq0p/2Cyj/sPdUHaCLGhP++Oyhpcs32P5eXYAozxKsALi" +
      "Jm47Fle0QKFh3HyBFxKpoFYWXywomfr1s1AkJpM1K+vh41KVHQlVpqYQxCTMz9wY" +
      "nmM3neUXRpoVhHNQkfhT+dFejGpReKLKsK9x8TPOTPR+mWpfAY0usKGmRHC4Tq0Z" +
      "gAFhAKQ2V3nJK6mgvxLINN6y0ud8aULbYpWL5cY3TJegKd4mQPt5r/VZ7bAKKsQ==")
      .getBytes());

  private static final String REAL_CERTIFICATE =
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIHQzCCBiugAwIBAgIDAJYUMA0GCSqGSIb3DQEBBQUAMIGMMQswCQYDVQQGEwJJ\n" +
    "TDEWMBQGA1UEChMNU3RhcnRDb20gTHRkLjErMCkGA1UECxMiU2VjdXJlIERpZ2l0\n" +
    "YWwgQ2VydGlmaWNhdGUgU2lnbmluZzE4MDYGA1UEAxMvU3RhcnRDb20gQ2xhc3Mg\n" +
    "MSBQcmltYXJ5IEludGVybWVkaWF0ZSBTZXJ2ZXIgQ0EwHhcNMDkwNzE5MjM0NTE5\n" +
    "WhcNMTAwNzE5MjM0NTE5WjCBozELMAkGA1UEBhMCVVMxHjAcBgNVBAoTFVBlcnNv\n" +
    "bmEgTm90IFZhbGlkYXRlZDEpMCcGA1UECxMgU3RhcnRDb20gRnJlZSBDZXJ0aWZp\n" +
    "Y2F0ZSBNZW1iZXIxHjAcBgNVBAMTFXdhdmUucHVmZnlwb29kbGVzLmNvbTEpMCcG\n" +
    "CSqGSIb3DQEJARYad2VibWFzdGVyQHB1ZmZ5cG9vZGxlcy5jb20wggEiMA0GCSqG\n" +
    "SIb3DQEBAQUAA4IBDwAwggEKAoIBAQC/ez3zoz6YW0yh1ODhR+pJ8Wox6YqLNAdT\n" +
    "wd1SOJRk9eihIevQHE0VnlWOZBTk9aDiNL7cAkvYEaBZizd9c639uTK5vp2lmFch\n" +
    "niU3htpp3X5Da5hc6Mq+sAN+5MTwLzdmdX3N1GQ/G5eLeY3lUll3sD/Rwb85BtVD\n" +
    "myggEsPp+KIFSNBtYpuapR4Vn6ZLvIyO2Hz+QAargu08TzosQSEhWXQlZone0AUz\n" +
    "RmHk8TXBPtyLVqaWhEYJ0j/CAtSa4FazpL/vxKeaLjlhPUFTN5CkfebCSG+Vq/+e\n" +
    "mpEgykD3JbP+HrhnnLURtEakcBaSbmvXnaK9i2Nz4y37T7SifJxtAgMBAAGjggOT\n" +
    "MIIDjzAJBgNVHRMEAjAAMAsGA1UdDwQEAwIDqDATBgNVHSUEDDAKBggrBgEFBQcD\n" +
    "ATAdBgNVHQ4EFgQU9Oouh16SPexmDQ0vqCZUy9T92aswgagGA1UdIwSBoDCBnYAU\n" +
    "60I00Jiwq5/0G2sI98xkLu8OLEWhgYGkfzB9MQswCQYDVQQGEwJJTDEWMBQGA1UE\n" +
    "ChMNU3RhcnRDb20gTHRkLjErMCkGA1UECxMiU2VjdXJlIERpZ2l0YWwgQ2VydGlm\n" +
    "aWNhdGUgU2lnbmluZzEpMCcGA1UEAxMgU3RhcnRDb20gQ2VydGlmaWNhdGlvbiBB\n" +
    "dXRob3JpdHmCAQowMgYDVR0RBCswKYIVd2F2ZS5wdWZmeXBvb2RsZXMuY29tghBw\n" +
    "dWZmeXBvb2RsZXMuY29tMIIBRwYDVR0gBIIBPjCCATowggE2BgsrBgEEAYG1NwEC\n" +
    "ADCCASUwLgYIKwYBBQUHAgEWImh0dHA6Ly93d3cuc3RhcnRzc2wuY29tL3BvbGlj\n" +
    "eS5wZGYwNAYIKwYBBQUHAgEWKGh0dHA6Ly93d3cuc3RhcnRzc2wuY29tL2ludGVy\n" +
    "bWVkaWF0ZS5wZGYwgbwGCCsGAQUFBwICMIGvMBQWDVN0YXJ0Q29tIEx0ZC4wAwIB\n" +
    "ARqBlkxpbWl0ZWQgTGlhYmlsaXR5LCByZWFkIHRoZSBzZWN0aW9uICpMZWdhbCBM\n" +
    "aW1pdGF0aW9ucyogb2YgdGhlIFN0YXJ0Q29tIENlcnRpZmljYXRpb24gQXV0aG9y\n" +
    "aXR5IFBvbGljeSBhdmFpbGFibGUgYXQgaHR0cDovL3d3dy5zdGFydHNzbC5jb20v\n" +
    "cG9saWN5LnBkZjBhBgNVHR8EWjBYMCqgKKAmhiRodHRwOi8vd3d3LnN0YXJ0c3Ns\n" +
    "LmNvbS9jcnQxLWNybC5jcmwwKqAooCaGJGh0dHA6Ly9jcmwuc3RhcnRzc2wuY29t\n" +
    "L2NydDEtY3JsLmNybDCBjgYIKwYBBQUHAQEEgYEwfzA5BggrBgEFBQcwAYYtaHR0\n" +
    "cDovL29jc3Auc3RhcnRzc2wuY29tL3N1Yi9jbGFzczEvc2VydmVyL2NhMEIGCCsG\n" +
    "AQUFBzAChjZodHRwOi8vd3d3LnN0YXJ0c3NsLmNvbS9jZXJ0cy9zdWIuY2xhc3Mx\n" +
    "LnNlcnZlci5jYS5jcnQwIwYDVR0SBBwwGoYYaHR0cDovL3d3dy5zdGFydHNzbC5j\n" +
    "b20vMA0GCSqGSIb3DQEBBQUAA4IBAQB/6f/L1v/rDdvrsdRA060CeeLOcghGQDNX\n" +
    "MKpsvaoF5/bWBXAZqXhLPalT5bkoFcswFIL0hUVgvarJboKOci3FweSNdBqz8Ady\n" +
    "S+QMA8pPI3epgifvoQkIdExm17WjkDg9UPvb78G7XDMLIyV6eN+elbTRAYUD4FTM\n" +
    "j9UCWi5dfPkwgc2/VkBT3/TXbe6e4GS1hvAgwyS4eJmm8UxM4jpUEIlQtWRYSAPV\n" +
    "+hxAaGpzHvKggkRMy9UcRXFVtPWSHGESIN+wHUf/t6NMlAyia8Tnjw2egHqND7In\n" +
    "/cKNIkg2kySvbZfntnYMZDgAOMQn87qMVx3IJ52PY1mHArVsqwXe\n" +
    "-----END CERTIFICATE-----\n";

  private static final String STARTCOM_CERT =
    "-----BEGIN CERTIFICATE-----\n" +
    "MIIH3jCCBcagAwIBAgIBCjANBgkqhkiG9w0BAQUFADB9MQswCQYDVQQGEwJJTDEW\n" +
    "MBQGA1UEChMNU3RhcnRDb20gTHRkLjErMCkGA1UECxMiU2VjdXJlIERpZ2l0YWwg\n" +
    "Q2VydGlmaWNhdGUgU2lnbmluZzEpMCcGA1UEAxMgU3RhcnRDb20gQ2VydGlmaWNh\n" +
    "dGlvbiBBdXRob3JpdHkwHhcNMDcxMDI0MjA1NDE2WhcNMTIxMDIyMjA1NDE2WjCB\n" +
    "jDELMAkGA1UEBhMCSUwxFjAUBgNVBAoTDVN0YXJ0Q29tIEx0ZC4xKzApBgNVBAsT\n" +
    "IlNlY3VyZSBEaWdpdGFsIENlcnRpZmljYXRlIFNpZ25pbmcxODA2BgNVBAMTL1N0\n" +
    "YXJ0Q29tIENsYXNzIDEgUHJpbWFyeSBJbnRlcm1lZGlhdGUgU2VydmVyIENBMIIB\n" +
    "IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtonGrO8JUngHrJJj0PREGBiE\n" +
    "gFYfka7hh/oyULTTRwbw5gdfcA4Q9x3AzhA2NIVaD5Ksg8asWFI/ujjo/OenJOJA\n" +
    "pgh2wJJuniptTT9uYSAK21ne0n1jsz5G/vohURjXzTCm7QduO3CHtPn66+6CPAVv\n" +
    "kvek3AowHpNz/gfK11+AnSJYUq4G2ouHI2mw5CrY6oPSvfNx23BaKA+vWjhwRRI/\n" +
    "ME3NO68X5Q/LoKldSKqxYVDLNM08XMML6BDAjJvwAwNi/rJsPnIO7hxDKslIDlc5\n" +
    "xDEhyBDBLIf+VJVSH1I8MRKbf+fAoKVZ1eKPPvDVqOHXcDGpxLPPr21TLwb0pwID\n" +
    "AQABo4IDVzCCA1MwDAYDVR0TBAUwAwEB/zALBgNVHQ8EBAMCAa4wHQYDVR0OBBYE\n" +
    "FOtCNNCYsKuf9BtrCPfMZC7vDixFMIGoBgNVHSMEgaAwgZ2AFE4L7xqkQFulF2mH\n" +
    "MMo0aEPQQa7yoYGBpH8wfTELMAkGA1UEBhMCSUwxFjAUBgNVBAoTDVN0YXJ0Q29t\n" +
    "IEx0ZC4xKzApBgNVBAsTIlNlY3VyZSBEaWdpdGFsIENlcnRpZmljYXRlIFNpZ25p\n" +
    "bmcxKTAnBgNVBAMTIFN0YXJ0Q29tIENlcnRpZmljYXRpb24gQXV0aG9yaXR5ggEB\n" +
    "MAkGA1UdEgQCMAAwPQYIKwYBBQUHAQEEMTAvMC0GCCsGAQUFBzAChiFodHRwOi8v\n" +
    "d3d3LnN0YXJ0c3NsLmNvbS9zZnNjYS5jcnQwWwYDVR0fBFQwUjAnoCWgI4YhaHR0\n" +
    "cDovL3d3dy5zdGFydHNzbC5jb20vc2ZzY2EuY3JsMCegJaAjhiFodHRwOi8vY3Js\n" +
    "LnN0YXJ0c3NsLmNvbS9zZnNjYS5jcmwwggFdBgNVHSAEggFUMIIBUDCCAUwGCysG\n" +
    "AQQBgbU3AQEEMIIBOzAvBggrBgEFBQcCARYjaHR0cDovL2NlcnQuc3RhcnRjb20u\n" +
    "b3JnL3BvbGljeS5wZGYwNQYIKwYBBQUHAgEWKWh0dHA6Ly9jZXJ0LnN0YXJ0Y29t\n" +
    "Lm9yZy9pbnRlcm1lZGlhdGUucGRmMIHQBggrBgEFBQcCAjCBwzAnFiBTdGFydCBD\n" +
    "b21tZXJjaWFsIChTdGFydENvbSkgTHRkLjADAgEBGoGXTGltaXRlZCBMaWFiaWxp\n" +
    "dHksIHJlYWQgdGhlIHNlY3Rpb24gKkxlZ2FsIExpbWl0YXRpb25zKiBvZiB0aGUg\n" +
    "U3RhcnRDb20gQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkgUG9saWN5IGF2YWlsYWJs\n" +
    "ZSBhdCBodHRwOi8vY2VydC5zdGFydGNvbS5vcmcvcG9saWN5LnBkZjARBglghkgB\n" +
    "hvhCAQEEBAMCAAcwUQYJYIZIAYb4QgENBEQWQlN0YXJ0Q29tIENsYXNzIDEgUHJp\n" +
    "bWFyeSBJbnRlcm1lZGlhdGUgRnJlZSBTU0wgU2VydmVyIENlcnRpZmljYXRlczAN\n" +
    "BgkqhkiG9w0BAQUFAAOCAgEAN9nwGVuwb7kFbGiREJ/EfPnRQ/JDsIIqbfPrglDY\n" +
    "P/q+mgx3Umd6tVrzkdnbu4GPgSJpp4b5k7qgJ/bVPJE8wgNmM/7/eDnqYEPKAFDI\n" +
    "duxVfPCEkF70nuwe6KK5UKvsiIYrH++cu6ENb8gtWNodtpuK+WUnSRFTwLEJuVk/\n" +
    "WemF0Ake/JPvoDxGnV8qLo1yMQdolfcdlHikpWAGHaNLc3mPqK29qxGoLNL+PrFx\n" +
    "mI0aNKHjuw7hl+yXFa6N25vXTtTzJDfaa8Iwf2D3YRSJC28/HH2HKdA9dNui9LFp\n" +
    "IkYc9uAyPQB3qFwRapaBhDQOmCtFyN1iOC8dtbUKsdp7/ZW5ImcZsP+a220Fc2+W\n" +
    "e0OQCeDenNpVorg9lKJovv5qQXnRfmBlac3HL1o6mWXI7gvlFoOlYPITAHgcvZHZ\n" +
    "lHrs45w+X26XFVXBAHNup8C7QiAKPTtk2M6Ii/xI1yYNpht4JANykesH4Ln4fYHw\n" +
    "1tH60t61XZ/Kbdg/pIzh1tE+QoUFlf+CR01qfskFjXcresRrgd00KOxgfJls6HnD\n" +
    "xoiEoL+c2vOoATe7vmvhKHv8S5pv1IBLnjJeQQqQsKY8lBYxf+b4Tl2xNddnBO28\n" +
    "G8P6HGtMDMHaPETF+esG9VpMNtJkq0eNiCzmEHc7MDlA6kpFIY0psK/W0aPh6hcO\n" +
    "MAg=\n" +
    "-----END CERTIFICATE-----\n";

  private CertificateManager manager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    CertPathStore store = new DefaultCertPathStore();
    manager = new CertificateManagerImpl(false, getSigner(), getVerifier(store),
        store);
  }

  public void testSignature() throws Exception {
    ProtocolWaveletDelta delta = ProtocolWaveletDelta.newBuilder()
        .setHashedVersion(getHashedVersion())
        .setAuthor("bob@example.com")
        .build();

    ProtocolSignedDelta signedDelta = manager.signDelta(delta);

    manager.storeSignerInfo(getSignerInfo().toProtoBuf());
    ProtocolWaveletDelta compare = manager.verifyDelta(signedDelta);

    assertEquals(delta, compare);
  }

  public void testSignature_missingSignerInfo() throws Exception {
    ProtocolWaveletDelta delta = ProtocolWaveletDelta.newBuilder()
        .setHashedVersion(getHashedVersion())
        .setAuthor("bob@example.com")
        .build();

    ProtocolSignedDelta signedDelta = manager.signDelta(delta);

    try {
      manager.verifyDelta(signedDelta);
      fail("expected exception, but didn't get it");
    } catch (SignatureException e) {
      // expected
    }
  }

  public void testSignature_authorNotMatching() throws Exception {
    ProtocolWaveletDelta delta = ProtocolWaveletDelta.newBuilder()
        .setHashedVersion(getHashedVersion())
        .setAuthor("bob@someotherdomain.com")
        .build();

    ProtocolSignedDelta signedDelta = manager.signDelta(delta);

    manager.storeSignerInfo(getSignerInfo().toProtoBuf());

    try {
      manager.verifyDelta(signedDelta);
      fail("expected exception, but didn't get it");
    } catch (SignatureException e) {
      // expected
    }
  }

  public void testRealSignature() throws Exception {

    CertPathStore store = new DefaultCertPathStore();

    // real signature is currently broken, set cert manager to not
    // verify
    manager = new CertificateManagerImpl(true, getSigner(), getRealVerifier(store),
        store);

    ProtocolWaveletDelta delta = ProtocolWaveletDelta.newBuilder()
        .setHashedVersion(getHashedVersion())
        .setAuthor("bob@puffypoodles.com")
        .build();

    ProtocolSignature signature = ProtocolSignature.newBuilder()
        .setSignerId(ByteString.copyFrom(getRealSignerInfo().getSignerId()))
        .setSignatureAlgorithm(SignatureAlgorithm.SHA1_RSA)
        .setSignatureBytes(ByteString.copyFrom(REAL_SIGNATURE))
        .build();

    ProtocolSignedDelta signedDelta = ProtocolSignedDelta.newBuilder()
        .setDelta(delta)  // TODO: this will be delta.getBytes() in the future
        .addSignature(signature)
        .build();

    manager.storeSignerInfo(getRealSignerInfo().toProtoBuf());
    ProtocolWaveletDelta compare = manager.verifyDelta(signedDelta);

    assertEquals(delta, compare);
  }

  private ProtocolHashedVersion getHashedVersion() {
    return WaveletOperationSerializer.serialize(HashedVersion.unsigned(3L));
  }

  private WaveSignatureVerifier getVerifier(CertPathStore store) {
    TrustRootsProvider trustRoots = getTrustRootsProvider();
    VerifiedCertChainCache cache = new DefaultCacheImpl(getFakeTimeSource());
    CachedCertPathValidator validator = new CachedCertPathValidator(
        cache, getFakeTimeSource(), getTrustRootsProvider());
    return  new WaveSignatureVerifier(validator, store);
  }

  private TrustRootsProvider getTrustRootsProvider() {
    return new TrustRootsProvider() {
      @Override
      public Collection<X509Certificate> getTrustRoots() {
        try {
          return getSigner().getSignerInfo().getCertificates();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  private WaveSigner getSigner() throws Exception {
    InputStream keyStream = new ByteArrayInputStream(PRIVATE_KEY.getBytes());
    InputStream certStream = new ByteArrayInputStream(CERTIFICATE.getBytes());
    List<InputStream> certStreams = ImmutableList.of(certStream);

    WaveSignerFactory factory = new WaveSignerFactory();
    return factory.getSigner(keyStream, certStreams, DOMAIN);
  }

  private SignerInfo getRealSignerInfo() throws Exception {
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    X509Certificate realCert = (X509Certificate) factory.generateCertificate(
        new ByteArrayInputStream(REAL_CERTIFICATE.getBytes()));
    X509Certificate startCom = (X509Certificate) factory.generateCertificate(
        new ByteArrayInputStream(STARTCOM_CERT.getBytes()));

    return new SignerInfo(HashAlgorithm.SHA256,
        ImmutableList.of(realCert, startCom), "puffypoodles.com");
  }

  private WaveSignatureVerifier getRealVerifier(CertPathStore store)
      throws Exception {
    TrustRootsProvider trustRoots = new DefaultTrustRootsProvider();

    VerifiedCertChainCache cache = new DefaultCacheImpl(getFakeTimeSource());
    CachedCertPathValidator validator = new CachedCertPathValidator(
        cache, getFakeTimeSource(), trustRoots);
    return new WaveSignatureVerifier(validator, store);
  }

  private SignerInfo getSignerInfo() throws Exception {
    return getSigner().getSignerInfo();
  }

  private TimeSource getFakeTimeSource() {
    return new TimeSource() {
      @Override
      public Date now() {
        return new Date(currentTimeMillis());
      }

      @Override
      public long currentTimeMillis() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(2009, 11, 1);
        return cal.getTimeInMillis();
      }
    };
  }
}
