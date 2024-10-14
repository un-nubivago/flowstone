#! /bin/sh

cat README.md \
    | sed '
        s_img/Flowstone\_Showcase\_1.gif_https://cdn.modrinth.com/data/yT1pBihO/images/07ccd5349bc5e3fbce3dd12a2c4def1398ecc490.gif_;'
