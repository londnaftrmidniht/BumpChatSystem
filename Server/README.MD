[Install MariaDB]
sudo apt-get update
sudo apt -y install software-properties-common dirmngr
sudo apt-key adv --recv-keys --keyserver keyserver.ubuntu.com 0xF1656F24C74CD1D8
sudo add-apt-repository 'deb [arch=amd64] http://mariadb.mirror.liquidtelecom.com/repo/10.4/debian stretch main'

[File Key Management Encryption Plugin to /etc/mysql/my.cnf]
>[mariadb]
>plugin_load_add = file_key_management

[Add keys to keyfile]
$sudo mkdir /etc/mysql/encryption
$sudo touch /etc/mysql/encryption/keyfile
$sudo su
#sudo openssl rand -hex 32 >> /etc/mysql/encryption/keyfile
#sudo openssl rand -hex 32 >> /etc/mysql/encryption/keyfile
#sudo openssl rand -hex 32 >> /etc/mysql/encryption/keyfile
#sudo openssl rand -hex 32 >> /etc/mysql/encryption/keyfile
#sudo openssl rand -hex 32 >> /etc/mysql/encryption/keyfile

-Go into file and add identified to each key ex: 1;generatedkey
#sudo nano /etc/mysql/encryption/keyfile

[Encrypt keyfile]
#sudo openssl rand -hex 128 > /etc/mysql/encryption/keyfile.key
#sudo openssl enc -aes-256-cbc -md sha1 \
   -pass file:/etc/mysql/encryption/keyfile.key \
   -in /etc/mysql/encryption/keyfile \
   -out /etc/mysql/encryption/keyfile.enc

[Add keyfile to /etc/mysql/my.cnf]
>[mariadb]
>plugin_load_add = file_key_management
>file_key_management_filename = /etc/mysql/encryption/keyfile.enc
>file_key_management_filekey = FILE:/etc/mysql/encryption/keyfile.key
>file_key_management_encryption_algorithm = AES_CTR
>encrypt_tmp_files=ON
>encrypt_binlog=ON

#systemctl restart mysql.service

[Add SSL cert to server]
sudo add-apt-repository 'deb http://ftp.debian.org/debian stretch-backports main'

sudo apt install python-certbot-apache -t stretch-backports
sudo certbot --apache -d bumpchat.adroitwebdesigns.com