/*
 
 */
package jtorrentutil;

import com.turn.ttorrent.client.*;
import com.turn.ttorrent.common.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.slf4j.*;

/**
 *
 * @author l4l
 */
public class JTorrentUtil {
    private static final Logger log = LoggerFactory.getLogger(JTorrentUtil.class);

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: JTorrentUtil torrentfile path_to_dest_folder path_to_source_folder");
            return;
        }
        
        long filesInTorrent = 0;
        long filesInTorrentSize = 0;
        long filesInSrcSymlinked = 0;
        long filesInSrcSymlinkedSize = 0;
        long filesInTorrentNotFound = 0;
        long filesInTorrentNotFoundSize = 0;
        
        //SharedTorrent torr = SharedTorrent.fromFile(new File("/home/l4l/Downloads/[kat.cr]sf.pulps.weird.tales.update.a.z.torrent"), null);
        SharedTorrent torr = SharedTorrent.fromFile(new File(args[0]), new File(args[1]));
        torr.stop();
        
        filesInTorrent = torr.getFiles().size();
        filesInTorrentSize = torr.getSize();
        
        File processFolder = new File(args[2]);
        
        Map<Long, Collection<File>> mapSizeToFiles = new HashMap<>();
        List<Torrent.TorrentFile> files = torr.getFiles();
        for (Torrent.TorrentFile file: files)
            if (null == mapSizeToFiles.get(file.size))
                mapSizeToFiles.put(file.size, new LinkedList<File>());
        
        mapSizeToFiles = processFolder(processFolder, mapSizeToFiles);
        
        for (Torrent.TorrentFile file: files) {
            Collection<File> filesFound = mapSizeToFiles.get(file.size);
            if (filesFound.isEmpty()) {
                filesInTorrentNotFound++;
                filesInTorrentNotFoundSize += file.size;
                continue;
            }

            Path dstPath = Paths.get(args[1], file.file.getPath()); // dest path + path inside torrent
            if (1 == filesFound.size()) {
                String srcPath = filesFound.iterator().next().getPath();
                log.info("LINKING:\t{}\tTO:\t{}", new Object[]{srcPath, dstPath});

                Files.createSymbolicLink(dstPath, Paths.get(srcPath));
                filesInSrcSymlinked++;
                filesInSrcSymlinkedSize += filesFound.iterator().next().length();
            } else {
                log.warn("More than 1 file with size {} bytes. Files = {}", new Object[]{file.size, filesFound.size()});
                
                int properNameCount = 0;
                for (File f : filesFound)
                    if (f.getName().equals(file.file.getName()))
                        properNameCount++;
                
                if (1 == properNameCount) {
                    for (File f : filesFound)
                        log.warn("\t{}", f.getAbsolutePath());
                    log.info("Choosing the only file with proper name: {}", file.file.getName());

                    for (File f : filesFound)
                        if (f.getName().equals(file.file.getName())) {
                            String srcPath = f.getPath();
                            log.info("LINKING:\t{}\tTO:\t{}", new Object[]{srcPath, dstPath});

                            Files.createSymbolicLink(dstPath, Paths.get(srcPath));
                            filesInSrcSymlinked++;
                            filesInSrcSymlinkedSize += f.length();
                        }
                } else {
                    log.warn("Calculating files hashes:");
                    Map<String, File> mapHashToFile = new HashMap<>(filesFound.size());
                    for (File f : filesFound) {
                        String md5;
                        try (FileInputStream fis = new FileInputStream(f)) {
                            md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
                        }
                        if (!mapHashToFile.containsKey(md5))
                            mapHashToFile.put(md5, f);
                        log.warn("\t{}\t{}", new Object[]{md5, f.getAbsolutePath()});
                    }
                    if (1 == mapHashToFile.size()) {
                        log.info("All files are the same. Choosing one of them: {}", filesFound.iterator().next().getPath());
                        Files.createSymbolicLink(dstPath, Paths.get(filesFound.iterator().next().getPath()));
                        filesInSrcSymlinked++;
                        filesInSrcSymlinkedSize += filesFound.iterator().next().length();
                    } else {
                        filesInTorrentNotFound++;
                        filesInTorrentNotFoundSize += file.size;

                        log.warn("All files are different. Mark as not found");
                    }
                }
            }
        }

        log.info("Files in torrent:\t{}\t({} bytes)", new Object[]{filesInTorrent, filesInTorrentSize}); 
        log.info("Files symlinked:\t{}\t({} bytes)", new Object[]{filesInSrcSymlinked, filesInSrcSymlinkedSize});
        log.info("Files not found:\t{}\t({} bytes)", new Object[]{filesInTorrentNotFound, filesInTorrentNotFoundSize});
    }

    /** Recursively lists files in [sub]folders by size in bytes, only sizes mentioned in map will be added (saves memory) */
    private static Map<Long, Collection<File>> processFolder(File folder, Map<Long, Collection<File>> map) {
        if (!folder.exists()) {
            log.error("Cannot find the folder: {}", folder.getPath());
            return map;
        }
        
        List<File> subFolders = new LinkedList<>();
        for (File subItem : folder.listFiles())
            if (subItem.isDirectory())
                subFolders.add(subItem);
            else {
                Collection<File> filesOfThatSize = map.get(subItem.length());
                if (null != filesOfThatSize)
                    filesOfThatSize.add(subItem);
            }
        
        for (File subFolder : subFolders)
            map = processFolder(subFolder, map);
        
        return map;
    }
    
}
