import sys
import os
import shutil

import boto3


if __name__ == '__main__':
    print(sys.argv)
    target_directory = sys.argv[1]
    output_name = sys.argv[2]

    target_bucket = sys.argv[3]
    prefix = sys.argv[4]

    path = os.path.abspath(os.path.join(os.getcwd(), target_directory))
    shutil.make_archive(output_name, 'zip', path)

    output_name = f'{output_name}.zip'

    client = boto3.client("s3")

    key_name = f'{prefix}{output_name}'
    client.upload_file(os.path.join('.', output_name), target_bucket, key_name)
