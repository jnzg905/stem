/*
 * Copyright 2014 Alexey Plotnik
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

package org.stem;

import org.junit.Before;
import org.junit.Ignore;

@Ignore
public class StreamingTest extends IntegrationTestBase {

    private int portIndex = 1;

    @Override
    protected String getStorageNodeConfigPath() {
        return "../stem-storagenode/src/test/resources/stem.small_ff.yaml";
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

//    @Test
//    public void test() throws Exception {
//        clusterManagerClient.computeMapping(); // first version of CRUSHmap
//        client.start();
//        List<byte[]> keysGenerated = generateStaticLoad(300);
//        debugKeysPositions(keysGenerated, "initial");
//        //joinPseudoNode(); // TODO: there we should wait for the real node
//        waitForExternalStorageNode("localhost:10000");
//        Layout layout = Layout.getInstance();
//        clusterManagerClient.computeMapping(); // second version of CRUSHmap
//        Thread.sleep(5000);
//        debugKeysPositions(keysGenerated, "after rebalancing");
//        CompactionManager.instance.performMajorCompaction();
//        debugKeysPositions(keysGenerated, "after compaction");
//
//        checkDataAvailability(keysGenerated);
//    }
//
//    private void checkDataAvailability(List<byte[]> keysGenerated) {
//        int i = 1;
//        int broken = 0;
//        List<ExtendedBlobDescriptor> brokenDescriptors = new ArrayList<ExtendedBlobDescriptor>();
//        for (byte[] keyOrig : keysGenerated) {
//            String endpoint = client.getFirstEndpointForKey(keyOrig);
//            ExtendedBlobDescriptor descriptor = client.getFirstDescriptorForKey(keyOrig);
//            byte[] data = client.get(keyOrig);
//            byte[] keyActual = DigestUtils.md5(data);
//
//            if (keyActual[0] != keyOrig[0]) {
//                broken++;
//                brokenDescriptors.add(descriptor);
//            }
//
//            Assert.assertArrayEquals(String.format("blob #%s[%s] (%s) located on %s is corrupted", i++, data.length, Hex.encodeHexString(keyOrig), endpoint), keyActual, keyOrig);
//        }
////        for (ExtendedBlobDescriptor d : brokenDescriptors)
////        {
////            System.out.println("0x"+Hex.encodeHexString(d.getKey()) + " is broken");
////        }
//    }
//
//    private void debugKeysPositions(List<byte[]> keysGenerated, String prefix) {
//        for (byte[] key : keysGenerated) {
//            String endpoint = client.getFirstEndpointForKey(key);
//            ExtendedBlobDescriptor descriptor = client.getFirstDescriptorForKey(key);
//            System.out.println(String.format("%s 0x%s, disk=%s, ff=%s, offset=%s, size=%s",
//                    prefix,
//                    Hex.encodeHexString(key),
//                    descriptor.getDisk(),
//                    descriptor.getFFIndex(),
//                    descriptor.getBodyOffset(),
//                    descriptor.getLength()));
//        }
//    }
//
//    private void waitForExternalStorageNode(String endpoint) throws Exception {
//        StorageNodeClient client = new StorageNodeClient(endpoint);
//        while (true) {
//            try {
//                client.start();
//                break;
//            } catch (IOException e) {
//                System.out.println(String.format("Node %s has not started yet", endpoint));
//                Thread.sleep(100);
//            }
//        }
//    }
//
//    private void joinPseudoNode() throws ZooException {
//        List<InetAddress> ipAddresses = Utils.getIpAddresses();
//        Map<UUID, MountPoint> mountPoints = new HashMap<UUID, MountPoint>();
//        UUID uuid = UUID.randomUUID();
//        mountPoints.put(
//                uuid,
//                new MountPoint(uuid, "/tmp/mp", (10l * 1024 * 1024 * 1024), new DataTracker(getvBucketsNum()))
//        );
//
//
//        JoinRequest req = new JoinRequest();
//        REST.StorageNode node = req.getNode();
//        node.setListen(StorageNodeDescriptor.getNodeListenAddress(), StorageNodeDescriptor.getNodeListenPort() + portIndex++);
//
//        for (InetAddress ipAddress : ipAddresses) {
//            node.getIpAddresses().add(ipAddress.toString());
//        }
//
//        for (MountPoint mp : mountPoints.values()) {
//            REST.Disk disk = new REST.Disk(
//                    mp.getId(),
//                    mp.getPath(),
//                    mp.getTotalSizeInBytes(),
//                    mp.getAllocatedSizeInBytes());
//            node.getDisks().add(disk);
//        }
//
//        clusterManagerClient.join(req, ZookeeperFactoryCached.newClient("localhost:2180"));
//    }
}
