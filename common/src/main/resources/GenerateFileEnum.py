import os
import sys

fileList = []
enumFn = "enumerated.txt"

def doDir(rootdir):
  for root, subFolders, files in os.walk(rootdir):
    abridgedRoot = root[len(rootdir)+1:]
    for file in files:
      if file != enumFn:
        if abridgedRoot == "":
          path = file
        else:
          path = abridgedRoot + "/" + file
        path = path.replace("\\", "/")  # normalize paths
        fileList.append(path)

  fileList.sort()

  with open(os.path.join(rootdir, enumFn), 'wb') as f:
    for file in fileList:
      f.write((u"%s\n" % file).encode('utf-8'))

doDir("defaultrc")
doDir("testrc")
