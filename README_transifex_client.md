*** Using transifex-client (tx)

This information is only useful to android-galaxyzoo app developers.
Translators should read README_translators.md instead.


* Push English strings to Transifex:

tx push -s


* Pull translations from Transifex:

tx pull

However, transifex currently wrongly escapes < and > and doesn't escape ',
so we need to fix that after a pull.
See
https://github.com/transifex/transifex/issues/267
and
https://github.com/transifex/transifex/issues/235


* Push translations to Transifex
  (though they should normally all come _from_ Transifex.)

tx push -t


