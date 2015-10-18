# JTorrentUtil
Avoid downloading duplicate files by creating a structure (folders and files) for a new .torrent, 
using soft links to existing files from hard drive.


I have a big collection of downloaded comixes, magazines and books in different file formats.

Most of that collection comes from an old torrent file with Pulp magazines inside.

Some time ago a crew that created that torrent, was created a new torrent, partially intersecting with old one, 
and partially intersected with the files I've already downloaded from the other sources (torrents/hostings).
I've created that software to avoid having duplicates of the same files, and to avoid breaking the structure of 
my old file collection.

The aim of that software is to prepare a folder for downloading a new torrent, populating it with the soft links to 
existing files, if they fit with size and contents (by calculating hashes of existing files).

Existing files are remained untouched in the process, new folder will get only soft links with 
proper names instead of files. After JTorrentUItil has created folder, you can use your favorite torrent software
to download remaining files.
