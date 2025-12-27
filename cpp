#include <iostream>
using namespace std;

int main()
{
    char text[100];
    int key;
    int choice;
    int i;

    while(true)
    {
        cout << "\n0. Exit\n";
        cout << "1. Encrypt\n";
        cout << "2. Decrypt\n";
        cout << "Enter choice: ";
        cin >> choice;

        if(choice == 0)
        {
            cout << "Program Ended";
            break;
        }

        cout << "Enter key (0 to 25): ";
        cin >> key;

        cin.ignore();   // buffer fix

        cout << "Enter text (CAPITAL letters only): ";
        cin.getline(text, 100);

        // ENCRYPT
        if(choice == 1)
        {
            for(i = 0 ; text[i] != '\0' ; i++)
            {
                if(text[i] >= 'A' && text[i] <= 'Z')
                {
                    text[i] = text[i] + key;

                    if(text[i] > 'Z')
                        text[i] = text[i] - 26;
                }
            }

            cout << "Encrypted text: " << text << endl;
        }

        // DECRYPT
        else if(choice == 2)
        {
            for(i = 0 ; text[i] != '\0' ; i++)
            {
                if(text[i] >= 'A' && text[i] <= 'Z')
                {
                    text[i] = text[i] - key;

                    if(text[i] < 'A')
                        text[i] = text[i] + 26;
                }
            }

            cout << "Decrypted text: " << text << endl;
        }

        else
        {
            cout << "Invalid choice\n";
        }
    }

    return 0;
}
