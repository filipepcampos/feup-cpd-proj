def get_int_from_stdin():
    try:
        return int(input())
    except ValueError:
        print("Please insert an integer: ", end="")
        return get_int_from_stdin()

def initialize_matrixes(size):
    ma = [1 for _ in range(size*size)]
    mb = [i + 1 for i in range(size) for _ in range(size)]
    mc = [0 for _ in range(size*size)]
    return ma, mb, mc

def onMult(size):
    ma, mb, mc = initialize_matrixes(size)

    for i in range(size):
        for j in range(size):
            for k in range(size):
                mc[i*size+j] += ma[i*size+k] * mb[k*size+j]
    
    return mc

def onMultLine(size):
    ma, mb, mc = initialize_matrixes(size)

    for i in range(size):
        for k in range(size):
            for j in range(size):
                mc[i*size+j] += ma[i*size+k] * mb[k*size+j]
    
    return mc

def onMultBlock(size, bkSize):
    ma, mb, mc = initialize_matrixes(size)

    num_blocks = int(size / bkSize)
    for block_y in range(num_blocks):
        block_y_index = block_y * bkSize * size
        for block_x in range(num_blocks):
            block_x_index = block_x * bkSize
            block_index = block_y_index + block_x_index

            for block_k in range(num_blocks):
                block_A_index = block_y_index + block_k * bkSize
                block_B_index = block_k * bkSize * size + block_x_index

                for i in range(bkSize):
                    for k in range(bkSize):
                        for j in range(bkSize):
                            mc[block_index+(i*size+j)] += ma[block_A_index+(i*size+k)] * mb[block_B_index+(k*size+j)]

    
    return mc

# Switch-case Alternative
options = {1: onMult, 2: onMultLine, 3: onMultBlock}

def main():
    # Selection Menu
    print("1. Multiplication\n2. Line Multiplication\n3. Block Multiplication")
    selection = 0
    while selection < 1 or selection > 3:
        print("Insert a valid option: ", end="")
        selection = get_int_from_stdin()
    
    # Size Selection
    print("Matrix size (lines = columns): ", end="")
    size = get_int_from_stdin()

    # Algorithm Choosen
    print(options[selection](size)) # TODO: ACCEPT BLOCK SIZE

if __name__ == "__main__":
    main()