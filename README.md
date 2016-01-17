# CacheQuiz

Два варианта Cache:

1. JDKCache - на основе LinkedHashMap (не thread safe)
2. CustomCache - "c нуля", претендует на thread safe (использует возможности StampedLock)

Тестироание CacheTester

# Использование

javac CacheTester.java                  // компиляция

1. java -ea -cp . CacheTester jdk       // тестирование JDKCache (в один поток)
2. java -ea -cp . CacheTester custom    // тестирование CustomCache (в один и несколько потоков)

ps. для запуска требуется Java 8
