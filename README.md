# ![](https://bitbucket.org/AndrewShpagin/pochelper/raw/master/images/outline_save_alt_black_18dp.png)[Download APK](https://github.com/AndrewShpagin/pochelper/raw/master/PocInside-apk-to-install.apk)

### WARNING! This app may not guarantee correct results of measurements! It just helps to understand what happens.
    
**Using the PocTech Inside app**

1. Download PocHelper-apk-to-install.apk (in the root of the repositiory). Run the application, allow access to files.
2. In the PocTech Care Mobile application, go to the Data, Review & Export Data tab, select a sensor, click Export.
3. Go to the PocTech Inside app. You will see a graph and the current value of sugar (BS). Each time you export, the graph is auto-updated. 
4. The value will be displayed even before you first calibrate the sensor. However, be careful, this is just an estimate.
5. The program is based on the use of the formula
\
**BS = (Iw-Ib) / K**
\
Where K is the sensitivity of the sensor, Iw is the glucose-dependent current, Ib is the base (glucose-independent) current. These currents are visible in the PocTech Care Mobile app on the Data page. By default, we assume R = 5.5, but as the device works, this coefficient slowly changes. If you reuse the sensor, then you should know your K coefficient; it is displayed in the PocTech Inside application. Instead of the value 5.5, you can enter your own in the settings. Put [x] Override K and enter the value. It makes sense to set if there was no calibration yet. In the case of a correct calibration (on even sugar), it makes no big sense to enter your K.
6. You can see other useful graphs, select the ones you need from the drop-down list. POC1, POC2 - other calculation algorithms, you can try which ones are best for you.

Description of available graphs:

    *  BS is the non-smoothed value of sugar.
    *  Smooth BS - The smoothed sugar value.
    *  K - sensor sensitivity.
    *  Iw - glucose-dependent current
    *  Ib - glucose-independent current
    *  POC1 - Algorithm 1 (from PocTech).
    *  POC2 - Algorithm 2 (from PocTech).
    
    
======================================

**Использование приложения ”PocTech Inside”**

1. Загрузите PocHelper-apk-to-install.apk (в корне репозитория). Запустите приложение, разрешите доступ к файлам.
2. В приложении PocTech Care Mobile перейдите во вкладку Data, Review & Export Data, выберите сенсор, нажмите Export.
3. Перейдите в приложение PocTech Inside. Вы увидите график и текущее значение сахара (BS). Каждый раз при экспорте график авто-обновляется.
4. Значение будут показываться даже до того, как вы впервые откалибруете датчик. Однако, будьте осторожны, это лишь оценочное значение. 
5. Работа программы основана на использовании формулы 
\
**BS = (Iw-Ib)/K** 
\
Где К - чувствительность сенсора, Iw - глюкозозависимый ток, Ib - базовый (глюкозонезависимый) ток. Эти токи видны в приложении PocTech Care Mobile на странице Data. По умолчанию мы полагаем R=5.5, но по мере работы датчика этот коэффициент медленно меняется. Если вы повторно используете датчик, то свой коэффициент K вы должны знать, он простоянно показывается в приложении PocTech Inside. Вместо значения 5.5 вы можете ввести свое в настройках. Поставьте [x] Override K и введите значение. Это есть смысл днлать, если еще не было калибровки. В случае корректной калибровки (на ровном сахаре) нет смысла вводить свой K.
6. Вы можете посмотреть другие полезные графики, выберите в выпадающем списке нужные. POC1, POC2 - другие алгоритмы расчета, можете попробовать, какие лучше подходят именно вам.  

Описание доступных графиков: 

    *   BS - несглаженное значение сахара.
    *   Smooth BS - сглаженное значение сахара.
    *   K - чувствительность сенсора.
    *   Iw - глюкозозависимый ток
    *   Ib - глюкозонезависимый ток
    *   POC1 - алгоритм 1 (от PocTech).
    *   POC2 - алгоритм 2 (от PocTech). 

![Screenshot](https://bitbucket.org/AndrewShpagin/pochelper/raw/master/images/poc1.jpg)
