$if (f.repeated)$

  clear$f.capName$();
  $if (f.message)$
    for ($f.messageType$ field : $target$.$f.getter$()) {
      append$f.capName$(new $f.javaType$(field));
    }
  $else$
    for ($f.javaType$ field : $target$.$f.getter$()) {
      append$f.capName$(field);
    }
  $endif$

$else$

  $if (f.optional)$
  if ($target$.has$f.capName$()) {
  $endif$
    $f.setter$($target$.$f.getter$());
  $if (f.optional)$
  } else {
    clear$f.capName$();
  }
  $endif$

$endif$
