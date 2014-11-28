#!/usr/bin/env python

import sys
from xml.sax.saxutils import unescape


if __name__ == '__main__':
    filepathIn = sys.argv[1];
    filepathOut = sys.argv[2];

    with open(filepathIn, 'r') as fin, open(filepathOut, 'w') as fout:
        while True:
            line = fin.readline()
            if not line:
                break

            unescaped = unescape(line)
            fout.write(unescaped)


